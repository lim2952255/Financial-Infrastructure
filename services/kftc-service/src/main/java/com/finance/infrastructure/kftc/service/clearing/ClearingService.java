package com.finance.infrastructure.kftc.service.clearing;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import com.finance.infrastructure.kftc.client.bank.BankClient;
import com.finance.infrastructure.kftc.client.bok.BokClient;
import com.finance.infrastructure.kftc.client.bok.BokClient.SettlementResponse;
import com.finance.infrastructure.kftc.domain.clearing.ClearingCycle;
import com.finance.infrastructure.kftc.domain.clearing.NetPosition;
import com.finance.infrastructure.kftc.domain.payment.PaymentInstruction;
import com.finance.infrastructure.kftc.domain.payment.PaymentInstructionStatus;
import com.finance.infrastructure.kftc.domain.settlement.SettlementSubmission;
import com.finance.infrastructure.kftc.repository.ClearingCycleRepository;
import com.finance.infrastructure.kftc.repository.PaymentInstructionRepository;
import com.finance.infrastructure.kftc.repository.SettlementSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

// 입금 완료된 지급지시를 한 주기로 묶어 청산하고 BOK 결제까지 수행한다.
@Service
@RequiredArgsConstructor
public class ClearingService {

    private final ClearingCycleRepository clearingCycleRepository;
    private final PaymentInstructionRepository instructionRepository;
    private final SettlementSubmissionRepository submissionRepository;
    private final BokClient bokClient; // BOK에게 결제요청을 전송하는 클라이언트
    private final BankClient bankClient; // BANK에게 결제완료 정보를 전송하는 클라이언트

    // 청신 및 결제를 수행하는 메서드
    @Transactional
    public ClearingResult clearAndSettle(String cycleId) {
        // 새로 입력받은 cycleId에 대해서 이미 이전에 처리중이던 청산정보가 있는지를 조회한다.
        ClearingCycle existingCycle = clearingCycleRepository.findByCycleId(cycleId)
                .orElse(null);

        // 이미 이전에 청산후 결제중이던 결제정보가 있다면 해당 결제정보를 리턴한다.
        if (existingCycle != null) {
            SettlementSubmission existingSubmission = submissionRepository
                    .findByClearingCycle(existingCycle)
                    .orElseThrow();
            return ClearingResult.from(existingCycle, existingSubmission);
        }

        // 아직 cycleId가 null인 모든 instruction들을 조회한다(아직 어던 청산주기에도 포함되지않아 청산이 되지 않은 instruction들)
        List<PaymentInstruction> instructions = instructionRepository
                .findAllByStatusAndClearingCycleIsNullOrderByIdAsc(
                        PaymentInstructionStatus.RECEIVER_CREDITED
                );
        // 만약 청산해야할 instruction들이 하나도 없으면 예외를 발생시킨다.
        if (instructions.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "there are no payment instructions eligible for clearing"
            );
        }

        // 현재 청산을 진행할 cycleId를 등록한다, (멱등성 방지)
        ClearingCycle cycle = clearingCycleRepository.save(new ClearingCycle(cycleId));
        // cycleId가 없는 instruction들을 해당 청산주기에 포함시킨다.
        instructions.forEach(instruction -> instruction.assignTo(cycle));
        // 청산주기의 상태를 CLOSED로 설정해 현재 청산주기를 닫아 이후 들어오는 instruction들은 다음 청산주기에 포함되도록 설정한다.
        cycle.close();
        // 각 기관별 순포지션을 계산한다.
        cycle.calculate(calculateNetPositions(instructions));
        // 각 instruction들의 청산이 완료되었으므로 각 instruction들의 상태를 청산이 완료되었음을 나타내는 CLEAR로 변경한다.
        instructions.forEach(PaymentInstruction::clear);

        // 새로운 결제 배치 Id와 현재 청산정보를 담은 SettlementSubmission을 생성하고 저장한다.
        SettlementSubmission submission = submissionRepository.save(
                new SettlementSubmission(deterministicSubmissionId(cycleId), cycle)
        );
        if (cycle.getPositions().isEmpty()) {
            // 기관별 순포지션이 모두 0원이면 중앙은행 당좌예금에서 이동할 자금이 없다.
            submission.settleWithoutSubmission();
        } else {
            // 이후 새로 생성한 settlementSubmission을 BOK에 전송하기 떄문에 submission의 상태를 SUBMIT으로 변경한다.
            submission.submit();

            // BOK에게 현재 결제요청정보랑 청산주기정보를 전송하고, 결제결과를 응답받는다.
            SettlementResponse settlementResponse = bokClient.settle(submission, cycle);
            // 만약 결제결과의 상태가 SETTLED가 아니라면, BOK에서 결제에 실패했다는 의미이기 때문에 예외를 발생시킨다.
            if (!"SETTLED".equals(settlementResponse.status())) {
                throw new IllegalStateException("BOK did not complete the settlement");
            }

            // 결제가 완료되면 submission의 상태와 청산주기의 상태를 SETTLE로 변경한다.
            submission.settle();
        }

        // 결제가 필요 없거나 BOK 결제가 완료되면 지급지시를 최종 상태로 변경한다.
        instructions.forEach(PaymentInstruction::settle);
        // 참여은행들에게 결제성공결과를 알린다.
        notifyParticipatingBanks(cycle, submission, instructions);

        return ClearingResult.from(cycle, submission);
    }

    // 참여은행들에게 결제성공 결과를 전송한다.
    private void notifyParticipatingBanks(
            ClearingCycle cycle,
            SettlementSubmission submission,
            List<PaymentInstruction> instructions
    ) {
        // 각 은행기관 ID와 해당 은행이 참여한 지급지시 ID 목록을 관리하는 map을 생성한다.
        Map<InstitutionId, Set<String>> instructionIdsByInstitution = new TreeMap<>(
                Comparator.comparing(InstitutionId::value)
        );

        // instructions내의 모든 instruction들을 순회하며,각 은행기관이 참여한 지급지시 ID를 Map에 매핑하여 추가한다.
        for (PaymentInstruction instruction : instructions) {
            // 송신은행이 참여한 지급지시 ID를 Map에 매핑하여 추가한다.
            addInstruction(
                    instructionIdsByInstitution,
                    instruction.getSenderParticipant().getInstitutionId(),
                    instruction.getInstructionId()
            );
            // 수신은행이 참여한 지급지시 ID를 Map에 매핑하여 추가한다.
            addInstruction(
                    instructionIdsByInstitution,
                    instruction.getReceiverParticipant().getInstitutionId(),
                    instruction.getInstructionId()
            );
        }

        // Map에 매핑된 각 은행기관과 은행기관이 참여한 지급지시들을 순회하며, 각 은행기관에게 결제처리가 완료된 instruction목록 정보를 전송한다.
        // 이후 해당 정보를 받은 은행기관은 각 instruction과 대응되는 transaction의 상태를 settled로 update한다.
        instructionIdsByInstitution.forEach((institutionId, instructionIds) ->
                bankClient.notifySettlement(
                        institutionId,
                        cycle.getCycleId(),
                        submission.getSubmissionId(),
                        List.copyOf(instructionIds)
                )
        );
    }

    // 특정 은행기관이 참여한 지급지시를 Map에 추가한다.
    private static void addInstruction(
            Map<InstitutionId, Set<String>> instructionIdsByInstitution,
            InstitutionId institutionId,
            String instructionId
    ) {
        instructionIdsByInstitution
                .computeIfAbsent(institutionId, ignored -> new LinkedHashSet<>())
                .add(instructionId);
    }

    // 각 기관별 순포지션을 계산한다.
    private static List<NetPosition> calculateNetPositions(
            List<PaymentInstruction> instructions
    ) {
        // 각 금융기관별 순포지션을 저장할 Map을 생성한다.
        Map<InstitutionId, BigDecimal> totals = new HashMap<>();
        // instruction들을 조회하면서 기관별 순포지션을 계산한다.
        for (PaymentInstruction instruction : instructions) {
            // 송신기관의 금액을 차감한다.
            totals.merge(
                    instruction.getSenderParticipant().getInstitutionId(),
                    instruction.amount().amount().negate(), // 금액의 부호를 뒤집는다.
                    BigDecimal::add
            );
            // 수신기관의 금액을 증가한다.
            totals.merge(
                    instruction.getReceiverParticipant().getInstitutionId(),
                    instruction.amount().amount(),
                    BigDecimal::add
            );
        }

        // 만약 순포지션이 0원인 기관은 BOK에서 결제할 정보가 없기 때문에 필터링해서 제거한다.
        return totals.entrySet().stream()
                .filter(entry -> entry.getValue().signum() != 0)
                .sorted(Map.Entry.comparingByKey(
                        java.util.Comparator.comparing(InstitutionId::value)
                ))
                .map(entry -> new NetPosition(entry.getKey(), Money.of(entry.getValue())))
                .toList();
    }

    // 결제 배치 Id를 생성한다.
    private static String deterministicSubmissionId(String cycleId) {
        return UUID.nameUUIDFromBytes(
                ("SETTLEMENT:" + cycleId).getBytes(StandardCharsets.UTF_8)
        ).toString();
    }
}

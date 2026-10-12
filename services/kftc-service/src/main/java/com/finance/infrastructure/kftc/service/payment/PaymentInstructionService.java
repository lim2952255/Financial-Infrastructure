package com.finance.infrastructure.kftc.service.payment;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.kftc.client.bank.BankClient;
import com.finance.infrastructure.kftc.domain.participant.ClearingParticipant;
import com.finance.infrastructure.kftc.domain.payment.PaymentInstruction;
import com.finance.infrastructure.kftc.domain.payment.PaymentInstructionStatus;
import com.finance.infrastructure.kftc.repository.ClearingParticipantRepository;
import com.finance.infrastructure.kftc.repository.PaymentInstructionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

// 송신은행으로부터 지급지시를 접수하고 수신은행의 입금 완료를 중계한다.
@Service
@RequiredArgsConstructor
public class PaymentInstructionService {

    private final ClearingParticipantRepository participantRepository;
    private final PaymentInstructionRepository instructionRepository;
    private final BankClient bankClient; // 수신은행에게 입금지시를 전송해주는 RestClient

    @Transactional
    public PaymentInstructionResult receive(PaymentInstructionCommand command) {
        // KFTC의 ParticipantRepository에서 관리중인 송신은행과 수신은행을 조회한다.
        ClearingParticipant sender = findParticipant(command.senderInstitutionId());
        ClearingParticipant receiver = findParticipant(command.receiverInstitutionId());

        // 송신은행으로부터 받은 지급지시 요청정보를 이전에 처리하고 있었는지를 검사한다(멱등성 보장)
        PaymentInstruction instruction = instructionRepository
                .findBySenderParticipantAndRequestId(sender, command.requestId())
                .orElse(null);
        if (instruction == null) {
            // 송신은행으로부터 받은 지급지시 정보를 instructionRepository에 저장한다.
            instruction = instructionRepository.save(new PaymentInstruction(
                    // instructionRepository에 지급지시 정보를 저장할때, 멱등성을 보장하기 위해 instructionId를 생성한다.
                    deterministicInstructionId(sender, command.requestId()),
                    command.requestId(),
                    sender,
                    command.senderAccountNumber(),
                    receiver,
                    command.receiverAccountNumber(),
                    command.amount()
            ));
        } else {
            // 만약 동일한 requestId의 지급지시요청정보가 있었다면, 내용까지 같은지, 아니면 내용은 다른데 우연히 requestId가 중복된건지를 조사한다.
            requireSameInstruction(instruction, command);
        }

        // instruction의 상태가 RECEIVED라면 송신은행에게서 지급지시를 막 접수받은 상태를 의미한다.
        if (instruction.getStatus() == PaymentInstructionStatus.RECEIVED) {
            // 지급지시를 수신은행에 입금요청을 보낼 수 있도록 상태를 FORWARDED로 변경한다.
            instruction.forward();
        }
        // instruction의 상태가 FORWAREDED라면, KFTC가 수신은행에 입금지시를 보낼 수 있는 상태이므로 수신은행에게 입금지시를 전송한다.
        if (instruction.getStatus() == PaymentInstructionStatus.FORWARDED) {
            // KFTC가 수신은행에게 입금요청을 전송한다.
            bankClient.creditReceiver(instruction);
            // 수신은행에서 입금완료 응답을 받으면 instruction의 상태를 RECEIVER_CREDITED상태로 update한다.
            // instruction의 상태가 RECEIVER_CREDITED 상태가 되면, 이제 해당 지급지시는 청산주기에 포함되어 청산 및 결제 배치에 포함될 수 있다.
            instruction.confirmReceiverCredit();
        }
        // 수신은행의 입금요청 처리 결과를 리턴한다.
        return PaymentInstructionResult.from(instruction);
    }

    // participantRepository에서 KFTC와 연결되어 있는 기관을 조회한다.
    private ClearingParticipant findParticipant(
            InstitutionId institutionId
    ) {
        return participantRepository.findByInstitutionId(institutionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "clearing participant not found: " + institutionId.value()
                ));
    }

    // KFTC <-> 수신은행의 입금지시 요청의 멱등성을 방지하기 위해 InstructionId를 KFTC가 생성해서 등록한다.
    private static String deterministicInstructionId(
            ClearingParticipant sender,
            String requestId
    ) {
        String source = sender.getInstitutionId().value() + ":" + requestId;
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }

    // 멱등성을 보장하기 위해, 지급지시 요청에 담긴 requestId가 중복될떄, 이게 이전 요청정보를 다시받은것인지, 새로운 요청정보인데 우연히 requestId가 중복된건지를 검사한다.
    private static void requireSameInstruction(
            PaymentInstruction instruction,
            PaymentInstructionCommand command
    ) {
        boolean sameInstruction = instruction.getSenderParticipant().getInstitutionId()
                .equals(command.senderInstitutionId())
                && instruction.getSenderAccountNumber().equals(command.senderAccountNumber())
                && instruction.getReceiverParticipant().getInstitutionId()
                .equals(command.receiverInstitutionId())
                && instruction.getReceiverAccountNumber().equals(command.receiverAccountNumber())
                && instruction.amount().equals(command.amount());
        if (!sameInstruction) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "request id was already used for a different payment instruction"
            );
        }
    }
}

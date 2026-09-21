package com.financialblockchain.simulator.bank.repository.transfer;

import com.financialblockchain.simulator.bank.domain.transfer.OutgoingTransfer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 송금 진행 상태를 거래 ID별로 저장하고 조회한다. */
public interface OutgoingTransferRepository extends JpaRepository<OutgoingTransfer, Long> {

    Optional<OutgoingTransfer> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);
}

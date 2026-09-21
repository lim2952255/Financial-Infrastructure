package com.financialblockchain.simulator.bank.domain.transfer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutgoingTransferRepository extends JpaRepository<OutgoingTransfer, Long> {

    Optional<OutgoingTransfer> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);
}

package com.finance.infrastructure.bank.repository;

import com.finance.infrastructure.bank.domain.transaction.BankTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findAllByOrderByIdAsc();

    Optional<BankTransaction> findByRequestId(String requestId);

    Optional<BankTransaction> findByPaymentInstructionId(String paymentInstructionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select bankTransaction
            from BankTransaction bankTransaction
            where bankTransaction.paymentInstructionId in :instructionIds
            order by bankTransaction.id
            """)
    List<BankTransaction> findAllByPaymentInstructionIdInForUpdate(
            @Param("instructionIds") Collection<String> instructionIds
    );
}

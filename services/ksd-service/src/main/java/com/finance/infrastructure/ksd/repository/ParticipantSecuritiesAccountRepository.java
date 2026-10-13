package com.finance.infrastructure.ksd.repository;

import com.finance.infrastructure.ksd.domain.account.ParticipantSecuritiesAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantSecuritiesAccountRepository
        extends JpaRepository<ParticipantSecuritiesAccount, Long> {

    Optional<ParticipantSecuritiesAccount> findByAccountNumber(String accountNumber);

    Optional<ParticipantSecuritiesAccount> findByParticipant_Id(Long participantId);
}

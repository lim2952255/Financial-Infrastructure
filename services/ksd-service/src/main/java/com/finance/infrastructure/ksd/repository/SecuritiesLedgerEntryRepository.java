package com.finance.infrastructure.ksd.repository;

import com.finance.infrastructure.ksd.domain.ledger.SecuritiesLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecuritiesLedgerEntryRepository extends JpaRepository<SecuritiesLedgerEntry, Long> {

    List<SecuritiesLedgerEntry> findAllBySecuritiesPosition_IdOrderByCreatedAtAsc(Long securitiesPositionId);

    List<SecuritiesLedgerEntry> findAllByReferenceIdOrderByCreatedAtAsc(String referenceId);
}

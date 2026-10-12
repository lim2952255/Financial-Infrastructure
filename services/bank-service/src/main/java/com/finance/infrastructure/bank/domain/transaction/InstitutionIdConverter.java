package com.finance.infrastructure.bank.domain.transaction;

import com.finance.infrastructure.common.institution.InstitutionId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class InstitutionIdConverter implements AttributeConverter<InstitutionId, String> {

    // Java 객체를 DB에 저장할 때, InstitutionId("BANK_A") → "BANK_A"로 저장하게 변환한다.
    @Override
    public String convertToDatabaseColumn(InstitutionId institutionId) {
        return institutionId == null ? null : institutionId.value();
    }

    // DB 엔티티를 Java 애플리케이션에서 조회할때는 "BANK_A" → InstitutionId("BANK_A")로 변환하여 가져온다.
    @Override
    public InstitutionId convertToEntityAttribute(String value) {
        return value == null ? null : InstitutionId.of(value);
    }
}

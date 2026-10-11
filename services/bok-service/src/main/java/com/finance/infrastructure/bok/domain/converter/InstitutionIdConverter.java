package com.finance.infrastructure.bok.domain.converter;

import com.finance.infrastructure.common.institution.InstitutionId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class InstitutionIdConverter implements AttributeConverter<InstitutionId, String> {

    @Override
    public String convertToDatabaseColumn(InstitutionId institutionId) {
        return institutionId == null ? null : institutionId.value();
    }

    @Override
    public InstitutionId convertToEntityAttribute(String value) {
        return value == null ? null : InstitutionId.of(value);
    }
}

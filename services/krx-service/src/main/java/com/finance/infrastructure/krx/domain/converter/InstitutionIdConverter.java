package com.finance.infrastructure.krx.domain.converter;

import com.finance.infrastructure.common.institution.InstitutionId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// InstitutionId 값 객체와 데이터베이스 문자열 컬럼 사이의 변환을 담당하는 Converter
@Converter
public class InstitutionIdConverter implements AttributeConverter<InstitutionId, String> {

    // InstitutionId의 실제 기관 식별자 값을 데이터베이스에 저장한다.
    @Override
    public String convertToDatabaseColumn(InstitutionId institutionId) {
        return institutionId == null ? null : institutionId.value();
    }

    // 데이터베이스에 저장된 기관 식별자 문자열을 InstitutionId 값 객체로 복원한다.
    @Override
    public InstitutionId convertToEntityAttribute(String value) {
        return value == null ? null : InstitutionId.of(value);
    }
}

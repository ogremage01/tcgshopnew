package com.shop.common.jpa;

import java.time.Duration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * MariaDB/MySQL 등에서 {@link Duration}을 BIGINT(나노초)로 저장한다.
 */
@Converter(autoApply = false)
public class DurationNanosConverter implements AttributeConverter<Duration, Long> {

    @Override
    public Long convertToDatabaseColumn(Duration duration) {
        return duration == null ? null : duration.toNanos();
    }

    @Override
    public Duration convertToEntityAttribute(Long dbData) {
        return dbData == null ? null : Duration.ofNanos(dbData);
    }
}

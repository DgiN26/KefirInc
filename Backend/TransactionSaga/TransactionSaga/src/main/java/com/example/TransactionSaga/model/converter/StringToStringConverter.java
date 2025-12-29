package com.example.TransactionSaga.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringToStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? "" : attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? "" : dbData;
    }
}
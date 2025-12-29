package com.example.TransactionSaga.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Converter
public class MapToStringConverter implements AttributeConverter<Map<String, Integer>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Integer> attribute) {
        try {
            return attribute == null || attribute.isEmpty() ? "{}" :
                    objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Override
    public Map<String, Integer> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null || dbData.isEmpty() || "{}".equals(dbData) ?
                    new HashMap<>() :
                    objectMapper.readValue(dbData, new TypeReference<Map<String, Integer>>() {});
        } catch (IOException e) {
            return new HashMap<>();
        }
    }
}
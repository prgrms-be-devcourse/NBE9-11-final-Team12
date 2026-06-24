package com.sisibibi.api.domain.report.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class CustomPromptsConverter implements AttributeConverter<List<AiReportCustomPrompt>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<AiReportCustomPrompt>> CUSTOM_PROMPT_LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<AiReportCustomPrompt> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("customPrompts JSON 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public List<AiReportCustomPrompt> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }

        try {
            return OBJECT_MAPPER.readValue(dbData, CUSTOM_PROMPT_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("customPrompts JSON 역직렬화에 실패했습니다.", e);
        }
    }
}

package com.nitorcreations.nflow.engine.dao;

import static org.springframework.util.StringUtils.isEmpty;
import static org.springframework.util.StringUtils.trimWhitespace;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Named;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Named
public class JSONMapper {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @SuppressWarnings("unchecked")
  public Map<String, String> jsonToMap(String json) {
    if (isEmpty(trimWhitespace(json))) {
      return new LinkedHashMap<>();
    }
    try {
      return (Map<String, String>) objectMapper.readValue(json, Map.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("JSON parsing failed", e);
    }
  }

  public String mapToJson(Map<String, String> map) {
    if (map == null) {
      return "";
    }
    try {
      return objectMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("JSON generation failed", e);
    }
  }
}

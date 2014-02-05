package com.nitorcreations.nflow.engine.dao;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class JSONMapperTest {
  JSONMapper mapper = new JSONMapper();

  @Test
  public void nullMapIsConvertedToEmptyString() {
    assertEquals("", mapper.mapToJson(null));
  }

  @Test
  public void emptyJsonIsConvertedToEmptyMap() {
    assertEquals(emptyMap(), mapper.jsonToMap(null));
    assertEquals(emptyMap(), mapper.jsonToMap(" "));
  }

  @Test
  public void mapIsConvertedToJson() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("key1", "value1");
    map.put("key2", "1");
    map.put("key3", null);

    String json = mapper.mapToJson(map);
    assertNotNull(json);
    Map<String, String> newMap = mapper.jsonToMap(json);
    assertEquals("1", newMap.get("key2"));
    assertEquals(null, newMap.get("key3"));
    assertEquals(map, newMap);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failedJsonParsingThrowsIllegalArgumentException() {
    mapper.jsonToMap("x");
  }

  @Test(expected = IllegalArgumentException.class)
  public void failedJsonGeneratingThrowsIllegalArgumentException() {
    mapper.mapToJson(Collections.<String, String> singletonMap(null, null));
  }
}

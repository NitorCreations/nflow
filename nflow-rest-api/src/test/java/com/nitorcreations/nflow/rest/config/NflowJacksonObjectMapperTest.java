package com.nitorcreations.nflow.rest.config;

import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.Charset;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;

public class NflowJacksonObjectMapperTest {
  NflowJacksonObjectMapper mapper = new NflowJacksonObjectMapper();

  DateTime dateTime = new DateTime(2014, 5, 31, 12, 20, 12, DateTimeZone.UTC);
  LocalDate localDate = new LocalDate(2014, 5, 31);

  @Test
  public void dateTimeSerializesAndDeserializes() throws IOException {
    byte[] bytes = mapper.writeValueAsBytes(dateTime);
    assertEquals("\"2014-05-31T12:20:12Z\"", new String(bytes, Charset.forName("UTF8")));
    assertEquals(dateTime.toDateTime(UTC), mapper.readValue(bytes, DateTime.class));
  }

  @Test
  public void localTimeSerializesAndDeserializes() throws IOException {
    byte[] bytes = mapper.writeValueAsBytes(localDate);
    assertEquals("\"2014-05-31\"", new String(bytes, Charset.forName("UTF8")));
    assertEquals(localDate, mapper.readValue(bytes, LocalDate.class));
  }
}

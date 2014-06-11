package com.nitorcreations.nflow.rest.config;

import static com.fasterxml.jackson.core.Version.unknownVersion;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JacksonJodaModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  static final DateTimeFormatter localDateFormat = ISODateTimeFormat.date();
  static final DateTimeFormatter dateTimeFormat = ISODateTimeFormat.dateTime().withZoneUTC();

  public JacksonJodaModule() {
    super("JodaModule", unknownVersion());
    addSerializer(LocalDate.class, new LocalDateSerializer());
    addDeserializer(LocalDate.class, new LocalDateDeserializer());
    addSerializer(DateTime.class, new DateTimeSerializer());
    addDeserializer(DateTime.class, new DateTimeDeserializer());
  }

  static class LocalDateSerializer extends JsonSerializer<LocalDate> {
    @Override
    public void serialize(LocalDate value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonMappingException {
      jgen.writeString(value.toString(localDateFormat));
    }
  }

  static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      return localDateFormat.parseLocalDate(jp.getText());
    }
  }

  static class DateTimeSerializer extends JsonSerializer<DateTime> {
    @Override
    public void serialize(DateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
      jgen.writeString(value.toString(dateTimeFormat));
    }
  }

  static class DateTimeDeserializer extends JsonDeserializer<DateTime> {
    @Override
    public DateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      return dateTimeFormat.parseDateTime(jp.getText());
    }
  }
}

package com.nitorcreations.nflow.engine;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.core.Version.unknownVersion;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Component
@Primary
public class NflowJacksonObjectMapper extends ObjectMapper {
  private static final long serialVersionUID = 1L;

  static final DateTimeFormatter localDateFormat = ISODateTimeFormat.date();
  static final DateTimeFormatter dateTimeFormat = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();

  public NflowJacksonObjectMapper() {
    setSerializationInclusion(NON_EMPTY);
    SimpleModule module = new SimpleModule("JodaModule", unknownVersion());
    module.addSerializer(LocalDate.class, new LocalDateSerializer());
    module.addDeserializer(LocalDate.class, new LocalDateDeserializer());
    module.addSerializer(DateTime.class, new DateTimeSerializer());
    module.addDeserializer(DateTime.class, new DateTimeDeserializer());
    registerModule(module);
  }

  static class LocalDateSerializer extends JsonSerializer<LocalDate> {
    @Override
    public void serialize(
        LocalDate value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonMappingException {
      jgen.writeString(value.toString(localDateFormat));
    }
  }

  static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(
        JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      return localDateFormat.parseLocalDate(jp.getText());
    }
  }

  static class DateTimeSerializer extends JsonSerializer<DateTime> {
    @Override
    public void serialize(
        DateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
      jgen.writeString(value.toString(dateTimeFormat));
    }
  }

  static class DateTimeDeserializer extends JsonDeserializer<DateTime> {
    @Override
    public DateTime deserialize(
        JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      return dateTimeFormat.parseDateTime(jp.getText());
    }
  }
}

package io.nflow.rest.v1.config.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.joda.time.format.ISODateTimeFormat.dateTime;
import static org.joda.time.format.ISODateTimeFormat.dateTimeNoMillis;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.ParamConverter;

import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.rest.config.jaxrs.DateTimeParamConverterProvider;

@ExtendWith(MockitoExtension.class)
public class DateTimeParamConverterProviderTest {

  DateTimeParamConverterProvider provider;
  ParamConverter<DateTime> converter;

  @BeforeEach
  public void setup() {
    provider = new DateTimeParamConverterProvider();
    converter = provider.getConverter(DateTime.class, String.class, null);
  }

  @Test
  public void getConverterForDate() {
    assertThat(provider.getConverter(Date.class, String.class, null), nullValue());
  }

  @Test
  public void convertDatetimeNoMillisToString() {
    assertThat(converter.fromString("2014-01-01T00:00:00Z"), is(dateTimeNoMillis().parseDateTime("2014-01-01T00:00:00Z")));
  }

  @Test
  public void convertDatetimeMillisToString() {
    assertThat(converter.fromString("2014-01-01T00:00:00.000Z"), is(dateTime().parseDateTime("2014-01-01T00:00:00.000Z")));
  }

  @Test
  public void convertInvalidDatetimeToString() {
    BadRequestException thrown = assertThrows(BadRequestException.class, () -> converter.fromString("2014/01/01"));
    assertThat(thrown.getMessage(), CoreMatchers.containsString("Unrecognized date format: 2014/01/01"));
  }

  @Test
  public void convertNullDatetimeToNullString() {
    assertThat(converter.toString(null), is(nullValue()));
  }

  @Test
  public void convertEmptyAndNullStringToNullDateTime() {
    assertThat(converter.fromString(""), is(nullValue()));
    assertThat(converter.fromString(null), is(nullValue()));
  }
}

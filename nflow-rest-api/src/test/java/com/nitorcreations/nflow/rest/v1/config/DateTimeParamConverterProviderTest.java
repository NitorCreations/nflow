package com.nitorcreations.nflow.rest.v1.config;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.joda.time.format.ISODateTimeFormat.dateTime;
import static org.joda.time.format.ISODateTimeFormat.dateTimeNoMillis;
import static org.junit.Assert.assertThat;

import java.util.Date;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ext.ParamConverter;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.rest.config.DateTimeParamConverterProvider;

@RunWith(MockitoJUnitRunner.class)
public class DateTimeParamConverterProviderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  DateTimeParamConverterProvider provider;
  ParamConverter<DateTime> converter;

  @Before
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
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Unrecognized date format: 2014/01/01");
    converter.fromString("2014/01/01");
  }
}

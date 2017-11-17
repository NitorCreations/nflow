package io.nflow.rest.config.jaxrs;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.joda.time.format.ISODateTimeFormat.dateTime;
import static org.joda.time.format.ISODateTimeFormat.dateTimeNoMillis;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import org.joda.time.DateTime;

@Provider
public class DateTimeParamConverterProvider implements ParamConverterProvider {

  @SuppressWarnings("unchecked")
  @Override
  public <T> ParamConverter<T> getConverter(Class<T> type, Type genericType, Annotation[] annotations) {
    if (type.equals(DateTime.class)) {
      return (ParamConverter<T>) new DateTimeParamConverter();
    }
    return null;
  }

  static final class DateTimeParamConverter implements ParamConverter<DateTime> {
    @Override
    public DateTime fromString(String value) {
      if (isEmpty(value)) {
        return null;
      }
      try {
        return dateTimeNoMillis().parseDateTime(value);
      } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
        try {
          return dateTime().parseDateTime(value);
        } catch (IllegalArgumentException e2) {
          throw new BadRequestException(format("Unrecognized date format: %s", value), e2);
        }
      }
    }

    @Override
    public String toString(DateTime value) {
      if (value == null) {
        return null;
      }
      return value.toString();
    }
  }
}

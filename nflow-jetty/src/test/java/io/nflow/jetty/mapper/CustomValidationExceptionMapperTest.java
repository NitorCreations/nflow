package io.nflow.jetty.mapper;

import static java.util.Arrays.asList;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;

import org.apache.cxf.validation.ResponseConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CustomValidationExceptionMapperTest {

  private CustomValidationExceptionMapper exceptionMapper;

  @BeforeEach
  public void setup() {
    exceptionMapper = new CustomValidationExceptionMapper();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void constraintViolationExceptionCausesBadRequest() {
    Path violationPath = mock(Path.class);
    when(violationPath.toString()).thenReturn("violationPath");

    ConstraintViolation violation = mock(ConstraintViolation.class);
    when(violation.getRootBeanClass()).thenReturn(CustomValidationExceptionMapperTest.class);
    when(violation.getPropertyPath()).thenReturn(violationPath);
    when(violation.getMessage()).thenReturn("violationMessage");

    ConstraintViolationException exception = mock(ConstraintViolationException.class);
    when(exception.getConstraintViolations()).thenReturn(new LinkedHashSet(asList(violation)));
    try (Response response = exceptionMapper.toResponse(exception)) {
      assertThat(response.getStatus(), is(BAD_REQUEST_400));
      ErrorResponse error = (ErrorResponse) response.getEntity();
      assertThat(error.error, is("violationPath: violationMessage"));
    }
  }

  @Test
  public void responseConstraintViolationExceptionCausesInternalServerError() {
    ConstraintViolationException exception = mock(ResponseConstraintViolationException.class);
    when(exception.getMessage()).thenReturn("error");
    try (Response response = exceptionMapper.toResponse(exception)) {
      assertThat(response.getStatus(), is(INTERNAL_SERVER_ERROR_500));
      ErrorResponse error = (ErrorResponse) response.getEntity();
      assertThat(error.error, is("error"));
    }
  }

  @Test
  public void otherExceptionsCauseInternalServerException() {
    ValidationException exception = mock(ValidationException.class);
    when(exception.getMessage()).thenReturn("error");
    try (Response response = exceptionMapper.toResponse(exception)) {
      assertThat(response.getStatus(), is(INTERNAL_SERVER_ERROR_500));
      ErrorResponse error = (ErrorResponse) response.getEntity();
      assertThat(error.error, is("error"));
    }
  }
}

package com.nitorcreations.nflow.jetty.validation;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CustomValidationExceptionMapperTest {

  private CustomValidationExceptionMapper exceptionMapper;

  @Before
  public void setup() {
    exceptionMapper = new CustomValidationExceptionMapper();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void toResponseWithConstraintViolationExceptionWorks() {
    Path violationPath = mock(Path.class);
    when(violationPath.toString()).thenReturn("violationPath");

    ConstraintViolation violation = mock(ConstraintViolation.class);
    when(violation.getRootBeanClass()).thenReturn(CustomValidationExceptionMapperTest.class);
    when(violation.getPropertyPath()).thenReturn(violationPath);
    when(violation.getMessage()).thenReturn("violationMessage");

    ConstraintViolationException cex = mock(ConstraintViolationException.class);
    when(cex.getConstraintViolations()).thenReturn(new HashSet(asList(violation)));
    Response resp = exceptionMapper.toResponse(cex);
    assertThat(resp.getEntity().toString(), is("violationPath: violationMessage"));
  }

}

package com.nitorcreations.nflow.jetty.validation;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.validation.ResponseConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class CustomValidationExceptionMapper implements ExceptionMapper<ValidationException> {

  private static final Logger logger = LoggerFactory.getLogger(CustomValidationExceptionMapper.class);

  @Override
  public Response toResponse(ValidationException exception) {
    if (exception instanceof ConstraintViolationException) {
      final ConstraintViolationException constraint = (ConstraintViolationException) exception;
      final boolean isResponseException = constraint instanceof ResponseConstraintViolationException;
      StringBuilder sb = new StringBuilder();
      for (final ConstraintViolation<?> violation: constraint.getConstraintViolations()) {
        logger.warn("{}.{}: {}",violation.getRootBeanClass().getSimpleName(), violation.getPropertyPath(), violation.getMessage());
        sb.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append(", ");
      }
      sb.setLength(sb.length() - 2);
      if (isResponseException) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
      return Response.status(Response.Status.BAD_REQUEST).entity(sb).build();
    }
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
  }

}

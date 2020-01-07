package io.nflow.jetty.mapper;

import static java.util.stream.Collectors.joining;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.validation.ResponseConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nflow.rest.v1.msg.ErrorResponse;

@Provider
public class CustomValidationExceptionMapper implements ExceptionMapper<ValidationException> {

  static final Logger logger = LoggerFactory.getLogger(CustomValidationExceptionMapper.class);

  @Override
  public Response toResponse(ValidationException exception) {
    if (exception instanceof ConstraintViolationException && !(exception instanceof ResponseConstraintViolationException)) {
      ConstraintViolationException constraint = (ConstraintViolationException) exception;
      String error = constraint.getConstraintViolations().stream().map(violation -> {
        logger.warn("{}.{}: {}", violation.getRootBeanClass().getSimpleName(), violation.getPropertyPath(),
            violation.getMessage());
        return violation.getPropertyPath() + ": " + violation.getMessage();
      }).collect(joining(", "));
      return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(error)).build();
    }
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(exception.getMessage())).build();
  }

}

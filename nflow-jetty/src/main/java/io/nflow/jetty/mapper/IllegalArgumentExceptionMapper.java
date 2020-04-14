package io.nflow.jetty.mapper;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.nflow.rest.v1.msg.ErrorResponse;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
  @Override
  public Response toResponse(IllegalArgumentException e) {
    return status(BAD_REQUEST.getStatusCode()).entity(new ErrorResponse(e.getMessage())).build();
  }
}

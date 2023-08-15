package io.nflow.jetty.mapper;

import static jakarta.ws.rs.core.Response.status;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.nflow.rest.v1.msg.ErrorResponse;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
  @Override
  public Response toResponse(WebApplicationException e) {
    try (Response response = e.getResponse()) {
      return status(response.getStatus()).entity(new ErrorResponse(e.getMessage())).build();
    }
  }
}

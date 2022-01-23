package io.nflow.jetty.mapper;

import static javax.ws.rs.core.Response.status;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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

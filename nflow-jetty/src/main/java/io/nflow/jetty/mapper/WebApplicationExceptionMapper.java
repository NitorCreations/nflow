package io.nflow.jetty.mapper;

import static javax.ws.rs.core.Response.status;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
  @Override
  public Response toResponse(WebApplicationException e) {
    return status(e.getResponse().getStatus()).entity(e.getMessage()).build();
  }
}

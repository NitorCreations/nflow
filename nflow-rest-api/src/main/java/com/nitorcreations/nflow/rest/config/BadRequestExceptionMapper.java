package com.nitorcreations.nflow.rest.config;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {
  @Override
  public Response toResponse(BadRequestException e) {
    return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
  }
}

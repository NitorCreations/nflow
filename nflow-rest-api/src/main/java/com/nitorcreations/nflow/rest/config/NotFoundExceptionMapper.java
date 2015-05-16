package com.nitorcreations.nflow.rest.config;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
  @Override
  public Response toResponse(NotFoundException e) {
    return Response.status(NOT_FOUND).build();
  }
}

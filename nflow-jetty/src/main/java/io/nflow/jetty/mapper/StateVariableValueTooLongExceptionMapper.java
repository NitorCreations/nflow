package io.nflow.jetty.mapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.nflow.engine.workflow.executor.StateVariableValueTooLongException;

@Provider
public class StateVariableValueTooLongExceptionMapper implements ExceptionMapper<StateVariableValueTooLongException> {
  @Override
  public Response toResponse(StateVariableValueTooLongException e) {
    return Response.status(BAD_REQUEST).entity(new ErrorResponse(e.getMessage())).build();
  }
}

package io.nflow.rest.mapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.nflow.engine.workflow.executor.StateVariableValueTooLongException;
import io.nflow.rest.mapper.StateVariableValueTooLongExceptionMapper;
import io.nflow.rest.v1.msg.ErrorResponse;

public class StateVariableValueTooLongExceptionMapperTest {
  StateVariableValueTooLongExceptionMapper mapper = new StateVariableValueTooLongExceptionMapper();

  @Test
  public void exceptionIsMappedToBadRequest() {
    try (Response response = mapper.toResponse(new StateVariableValueTooLongException("error"))) {
      assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
      ErrorResponse error = (ErrorResponse) response.getEntity();
      assertThat(error.error, is("error"));
    }
  }
}

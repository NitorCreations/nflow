package io.nflow.jetty.mapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

public class WebApplicationExceptionMapperTest {
  WebApplicationExceptionMapper mapper = new WebApplicationExceptionMapper();

  @Test
  public void exceptionStatusAndMessageAreUsedInResponse() {
    try (Response response = mapper.toResponse(new WebApplicationException("error", BAD_REQUEST))) {
      assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
      ErrorResponse error = (ErrorResponse) response.getEntity();
      assertThat(error.error, is("error"));
    }
  }
}

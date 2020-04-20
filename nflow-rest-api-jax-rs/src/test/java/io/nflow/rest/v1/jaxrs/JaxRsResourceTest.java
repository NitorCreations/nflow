package io.nflow.rest.v1.jaxrs;

import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.nflow.engine.service.NflowNotFoundException;
import io.nflow.rest.v1.msg.ErrorResponse;

public class JaxRsResourceTest {

  private final JaxRsResource resource = new TestResource();

  @Test
  public void handleExceptionsReturnsResponseWhenSuccessful() {
    try (Response response = resource.handleExceptions(() -> ok("ok"))) {
      assertThat(response.getStatus(), is(OK.getStatusCode()));
      assertThat(response.readEntity(String.class), is("ok"));
    }
  }

  @Test
  public void handleExceptionsReturnsBadRequestForIllegalArgumentException() {
    try (Response response = resource.handleExceptions(() -> {
      throw new IllegalArgumentException("error");
    })) {
      assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
      assertThat(response.readEntity(ErrorResponse.class).error, is("error"));
    }
  }

  @Test
  public void handleExceptionsReturnsNotFoundForNflowNotFoundException() {
    try (Response response = resource.handleExceptions(() -> {
      throw new NflowNotFoundException("Item", 1, new Exception());
    })) {
      assertThat(response.getStatus(), is(NOT_FOUND.getStatusCode()));
      assertThat(response.readEntity(ErrorResponse.class).error, is("Item 1 not found"));
    }
  }

  @Test
  public void handleExceptionsReturnsInternalServerErrorForOtherThrowables() {
    try (Response response = resource.handleExceptions(() -> {
      throw new RuntimeException("error");
    })) {
      assertThat(response.getStatus(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
      assertThat(response.readEntity(ErrorResponse.class).error, is("error"));
    }
  }

  class TestResource extends JaxRsResource {
    // test resource
  }
}

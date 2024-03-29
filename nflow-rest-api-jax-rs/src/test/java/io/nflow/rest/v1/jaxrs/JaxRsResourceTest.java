package io.nflow.rest.v1.jaxrs;

import static jakarta.ws.rs.core.Response.ok;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.nflow.rest.v1.msg.ErrorResponse;

public class JaxRsResourceTest {

  private final JaxRsResource resource = new TestResource();

  @Test
  public void handleExceptionsReturnsResponseWhenSuccessful() {
    try (Response response = resource.handleExceptions(() -> ok("ok"))) {
      assertThat(response.getStatus(), is(OK.getStatusCode()));
      assertThat(response.getEntity(), is("ok"));
    }
  }

  @Test
  public void handleExceptionsReturnsResponseOnFailure() {
    try (Response response = resource.handleExceptions(() -> {
      throw new RuntimeException("error");
    })) {
      assertThat(response.getStatus(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
      assertThat(((ErrorResponse) response.getEntity()).error, is("error"));
    }
  }

  class TestResource extends JaxRsResource {
    // test resource
  }
}

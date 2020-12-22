package io.nflow.rest.v1.springweb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.ResponseEntity.ok;
import static reactor.core.publisher.Mono.just;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import io.nflow.rest.v1.msg.ErrorResponse;

public class SpringWebResourceTest {

  private final SpringWebResource resource = new TestResource();

  @Test
  public void handleExceptionsReturnsResponseWhenSuccessful() {
    ResponseEntity<?> response = resource.handleExceptions(() -> just(ok("ok"))).block();

    assertThat(response.getStatusCode(), is(OK));
    assertThat(response.getBody(), is("ok"));
  }

  @Test
  public void handleExceptionsReturnsResponseOnFailure() {
    ResponseEntity<?> response = resource.handleExceptions(() -> {
      throw new RuntimeException("error");
    }).block();

    assertThat(response.getStatusCode(), is(INTERNAL_SERVER_ERROR));
    assertThat(((ErrorResponse) response.getBody()).error, is("error"));
  }

  class TestResource extends SpringWebResource {
    // test resource
  }
}

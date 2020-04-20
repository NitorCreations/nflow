package io.nflow.rest.v1.springweb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.ResponseEntity.ok;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import io.nflow.engine.service.NflowNotFoundException;
import io.nflow.rest.v1.msg.ErrorResponse;

public class SpringWebResourceTest {

  private final SpringWebResource resource = new TestResource();

  @Test
  public void handleExceptionsReturnsResponseWhenSuccessful() {
    ResponseEntity<?> response = resource.handleExceptions(() -> ok("ok"));

    assertThat(response.getStatusCode(), is(OK));
    assertThat(response.getBody(), is("ok"));
  }

  @Test
  public void handleExceptionsReturnsBadRequestForIllegalArgumentException() {
    ResponseEntity<?> response = resource.handleExceptions(() -> {
      throw new IllegalArgumentException("error");
    });

    assertThat(response.getStatusCode(), is(BAD_REQUEST));
    assertThat(((ErrorResponse) response.getBody()).error, is("error"));
  }

  @Test
  public void handleExceptionsReturnsNotFoundForNflowNotFoundException() {
    ResponseEntity<?> response = resource.handleExceptions(() -> {
      throw new NflowNotFoundException("Item", 1, new Exception());
    });

    assertThat(response.getStatusCode(), is(NOT_FOUND));
    assertThat(((ErrorResponse) response.getBody()).error, is("Item 1 not found"));
  }

  @Test
  public void handleExceptionsReturnsInternalServerErrorForOtherThrowables() {
    ResponseEntity<?> response = resource.handleExceptions(() -> {
      throw new RuntimeException("error");
    });

    assertThat(response.getStatusCode(), is(INTERNAL_SERVER_ERROR));
    assertThat(((ErrorResponse) response.getBody()).error, is("error"));
  }

  class TestResource extends SpringWebResource {
    // test resource
  }
}

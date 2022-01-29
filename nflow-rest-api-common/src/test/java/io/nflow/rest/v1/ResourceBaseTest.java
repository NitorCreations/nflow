package io.nflow.rest.v1;

import static io.nflow.rest.v1.ResourceBase.INCLUDE_PARAM_DESC;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.nflow.engine.service.NflowNotFoundException;
import io.nflow.rest.v1.msg.ErrorResponse;

public class ResourceBaseTest {

  private final ResourceBase resource = new TestResource();

  @Test
  public void handleExceptionsReturnsResponseWhenSuccessful() {
    String response = resource.handleExceptions(() -> "ok", this::toError);
    assertThat(response, is("ok"));
  }

  @Test
  public void handleExceptionsReturnsBadRequestForIllagelStateException() {
    String response = resource.handleExceptions(() -> {
      throw new IllegalArgumentException("error");
    }, this::toError);
    assertThat(response, is("400 error"));
  }

  @Test
  public void handleExceptionsReturnsNotFoundForNflowNotFoundException() {
    String response = resource.handleExceptions(() -> {
      throw new NflowNotFoundException("Item", 1, new Exception());
    }, this::toError);
    assertThat(response, is("404 Item 1 not found"));
  }

  @Test
  public void handleExceptionsReturnsInternalServerErrorForOtherThrowables() {
    String response = resource.handleExceptions(() -> {
      throw new RuntimeException("error");
    }, this::toError);
    assertThat(response, is("500 error"));
  }

  @Test
  public void includeQueryParamDescriptionHasAllPossibleValuesDocumented() {
    stream(ApiWorkflowInstanceInclude.values())
        .forEach(include -> assertThat(INCLUDE_PARAM_DESC, containsString("* " + include + ": ")));
  }

  private String toError(int statusCode, ErrorResponse body) {
    return format("%s %s", statusCode, body.error);
  }

  class TestResource extends ResourceBase {
    // test resource
  }
}

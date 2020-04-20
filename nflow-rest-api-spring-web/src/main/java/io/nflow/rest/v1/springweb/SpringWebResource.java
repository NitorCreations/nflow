package io.nflow.rest.v1.springweb;

import static org.springframework.http.ResponseEntity.status;

import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;

import io.nflow.rest.v1.ResourceBase;
import io.nflow.rest.v1.msg.ErrorResponse;

public abstract class SpringWebResource extends ResourceBase {

  protected ResponseEntity<?> handleExceptions(Supplier<ResponseEntity<?>> response) {
    return handleExceptions(response::get, this::toErrorResponse);
  }

  private ResponseEntity<?> toErrorResponse(int statusCode, ErrorResponse body) {
    return status(statusCode).body(body);
  }
}

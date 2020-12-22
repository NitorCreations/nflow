package io.nflow.rest.v1.springweb;

import static org.springframework.http.ResponseEntity.status;
import static reactor.core.publisher.Mono.just;

import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;

import io.nflow.rest.v1.ResourceBase;
import io.nflow.rest.v1.msg.ErrorResponse;
import reactor.core.publisher.Mono;

public abstract class SpringWebResource extends ResourceBase {

  protected Mono<ResponseEntity<?>> handleExceptions(Supplier<Mono<ResponseEntity<?>>> response) {
    return handleExceptions(response::get, this::toErrorResponse);
  }

  private Mono<ResponseEntity<?>> toErrorResponse(int statusCode, ErrorResponse body) {
    return just(status(statusCode).body(body));
  }
}

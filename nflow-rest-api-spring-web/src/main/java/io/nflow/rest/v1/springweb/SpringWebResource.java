package io.nflow.rest.v1.springweb;

import static org.springframework.http.ResponseEntity.status;
import static reactor.core.publisher.Mono.just;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;

import io.nflow.rest.config.springweb.SchedulerService;
import io.nflow.rest.v1.ResourceBase;
import io.nflow.rest.v1.msg.ErrorResponse;
import reactor.core.publisher.Mono;

public abstract class SpringWebResource extends ResourceBase {

  private final SchedulerService scheduler;

  protected SpringWebResource(SchedulerService scheduler) {
    this.scheduler = scheduler;
  }

  protected Mono<ResponseEntity<?>> wrapBlocking(Callable<ResponseEntity<?>> callable) {
    return scheduler.callAsync(callable);
  }

  protected Mono<ResponseEntity<?>> handleExceptions(Supplier<Mono<ResponseEntity<?>>> response) {
    return handleExceptions(response::get, this::toErrorResponse);
  }

  private Mono<ResponseEntity<?>> toErrorResponse(int statusCode, ErrorResponse body) {
    return just(status(statusCode).body(body));
  }
}

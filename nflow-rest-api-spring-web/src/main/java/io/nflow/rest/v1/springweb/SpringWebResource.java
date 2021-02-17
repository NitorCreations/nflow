package io.nflow.rest.v1.springweb;

import io.nflow.rest.config.springweb.SchedulerService;
import io.nflow.rest.v1.ResourceBase;
import io.nflow.rest.v1.msg.ErrorResponse;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static org.springframework.http.ResponseEntity.status;
import static reactor.core.publisher.Mono.just;

public abstract class SpringWebResource extends ResourceBase {

  private final SchedulerService scheduler;

  protected SpringWebResource(SchedulerService scheduler) {
    this.scheduler = scheduler;
  }

  protected Mono<ResponseEntity<?>> wrapBlocking(Callable<ResponseEntity<?>> callable) {
    return scheduler.callAsync(callable);
  }

  protected Mono<ResponseEntity<?>> handleExceptions(Supplier<Mono<ResponseEntity<?>>> response) {
    return Mono.just(response)
        .flatMap(Supplier::get)
        .onErrorResume(ex -> toErrorResponse(resolveExceptionHttpStatus(ex), new ErrorResponse(ex.getMessage())));
  }

  private Mono<ResponseEntity<?>> toErrorResponse(int statusCode, ErrorResponse body) {
    return just(status(statusCode).body(body));
  }
}

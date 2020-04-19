package io.nflow.rest.v1.springweb;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.status;

import java.util.function.Supplier;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.nflow.rest.v1.ResourceBase;
import io.nflow.rest.v1.msg.ErrorResponse;

public abstract class SpringWebResource extends ResourceBase {

  protected ResponseEntity<?> handleExceptions(Supplier<ResponseEntity<?>> response, Object... args) {
    try {
      return response.get();
    } catch (IllegalArgumentException e) {
      return toErrorResponse(BAD_REQUEST, e.getMessage());
    } catch (@SuppressWarnings("unused") EmptyResultDataAccessException e) {
      return toErrorResponse(NOT_FOUND, format("%s not found", args));
    } catch (Throwable t) {
      return toErrorResponse(INTERNAL_SERVER_ERROR, t.getMessage());
    }
  }

  private ResponseEntity<?> toErrorResponse(HttpStatus status, String error) {
    return status(status).body(new ErrorResponse(error));
  }
}

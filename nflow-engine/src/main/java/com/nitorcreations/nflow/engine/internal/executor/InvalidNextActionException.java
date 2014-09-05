package com.nitorcreations.nflow.engine.internal.executor;

public class InvalidNextActionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidNextActionException(String message) {
    super(message);
  }
}

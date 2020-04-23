package io.nflow.engine.service;

import static java.lang.String.format;

public class NflowNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public NflowNotFoundException(String type, long id, Throwable cause) {
    super(format("%s %s not found", type, id), cause);
  }
}

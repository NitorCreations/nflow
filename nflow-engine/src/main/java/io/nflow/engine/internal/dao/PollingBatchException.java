package io.nflow.engine.internal.dao;

public class PollingBatchException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PollingBatchException(String msg) {
    super(msg, null, true, false);
  }

}

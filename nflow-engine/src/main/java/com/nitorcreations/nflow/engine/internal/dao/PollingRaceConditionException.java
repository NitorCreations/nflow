package com.nitorcreations.nflow.engine.internal.dao;

public class PollingRaceConditionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PollingRaceConditionException(String msg) {
    super(msg, null, true, false);
  }

}

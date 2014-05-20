package com.nitorcreations.nflow.engine;

public class Sandman {
  public static final long SHORT_DELAY_MS = 50;
  public static final long SMALL_DELAY_MS = SHORT_DELAY_MS * 5;
  public static final long MEDIUM_DELAY_MS = SHORT_DELAY_MS * 10;
  public static final long LONG_DELAY_MS = SHORT_DELAY_MS * 200;

  public static final void sleep(long delayMs) {
    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException e) {
    }
  }
}

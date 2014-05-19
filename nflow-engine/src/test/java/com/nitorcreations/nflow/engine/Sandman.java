package com.nitorcreations.nflow.engine;

public class Sandman {
  public static long SHORT_DELAY_MS = 50;
  public static long SMALL_DELAY_MS = SHORT_DELAY_MS * 5;
  public static long MEDIUM_DELAY_MS = SHORT_DELAY_MS * 10;
  public static long LONG_DELAY_MS = SHORT_DELAY_MS * 200;

  public static void sleep(long delayMs) {
    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException e) {
    }
  }
}

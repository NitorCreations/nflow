package com.nitorcreations.nflow.engine.internal.storage.db;

public interface SQLVariants {
  String currentTimePlusSeconds(int seconds);

  boolean hasUpdateReturning();

  String castToEnumType(String variable, String type);

  boolean hasUpdateableCTE();

  String least(String value1, String value2);

  String least1Param(String value1, String value2);

}

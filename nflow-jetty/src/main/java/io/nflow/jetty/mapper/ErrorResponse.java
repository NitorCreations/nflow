package io.nflow.jetty.mapper;

import io.nflow.engine.model.ModelObject;

public class ErrorResponse extends ModelObject {

  public String error;

  public ErrorResponse() {
    // empty constructor is required by jersey object mapping
  }

  public ErrorResponse(String error) {
    this.error = error;
  }

}

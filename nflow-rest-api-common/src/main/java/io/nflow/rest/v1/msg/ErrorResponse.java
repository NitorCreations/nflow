package io.nflow.rest.v1.msg;

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

package io.nflow.rest.v1.msg;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;

@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class ErrorResponse extends ModelObject {

  public String error;

  public ErrorResponse() {
    // empty constructor is required by jersey object mapping
  }

  public ErrorResponse(String error) {
    this.error = error;
  }

}

package com.nitorcreations.nflow.engine.workflow.data;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class WorkflowStateMethod {
  public final Method method;
  final StateParameter[] params;

  static class StateParameter {
    final String key;
    final Type type;
    final Object nullValue;
    final boolean readoOnly;

    public StateParameter(String key, Type type, Object nullValue, boolean readOnly) {
      this.key = key;
      this.type = type;
      this.nullValue = nullValue;
      readoOnly = readOnly;
    }
  }

  public WorkflowStateMethod(Method method, StateParameter[] params) {
    this.method = method;
    this.params = params;
  }
}

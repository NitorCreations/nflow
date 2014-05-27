package com.nitorcreations.nflow.engine.workflow;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class WorkflowStateMethod {
  public final Method method;
  public final StateParameter[] params;

  public static class StateParameter {
    public final String key;
    public final Type type;
    public final Object nullValue;
    public final boolean readoOnly;

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

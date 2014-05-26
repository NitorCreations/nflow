package com.nitorcreations.nflow.engine.workflow;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class WorkflowStateMethod {
  public final Method method;
  public final StateParameter[] params;

  public static class StateParameter {
    public final String key;
    public final Type type;

    public StateParameter(String key, Type type) {
      this.key = key;
      this.type = type;
    }
  }

  public WorkflowStateMethod(Method method, StateParameter[] params) {
    this.method = method;
    this.params = params;
  }
}

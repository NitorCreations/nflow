package io.nflow.engine.internal.workflow;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.stream.Stream;

import io.nflow.engine.model.ModelObject;

public class WorkflowStateMethod extends ModelObject {
  public final Method method;
  final StateParameter[] params;

  public boolean hasParameter(String name) {
    return Stream.of(params).anyMatch(p -> p.key.equals(name));
  }

  static class StateParameter extends ModelObject {
    final String key;
    final Type type;
    final Object nullValue;
    final boolean readOnly;
    final boolean mutable;

    public StateParameter(String key, Type type, Object nullValue, boolean readOnly, boolean mutable) {
      this.key = key;
      this.type = type;
      this.nullValue = nullValue;
      this.readOnly = readOnly;
      this.mutable = mutable;
    }
  }

  public WorkflowStateMethod(Method method, StateParameter... params) {
    this.method = method;
    this.params = params;
  }
}

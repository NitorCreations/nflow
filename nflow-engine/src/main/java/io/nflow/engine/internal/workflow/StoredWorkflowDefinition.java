package io.nflow.engine.internal.workflow;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;

public class StoredWorkflowDefinition extends ModelObject {

  public String type;
  public String description;
  public String onError;
  public List<State> states;
  public List<Signal> supportedSignals = new ArrayList<>();

  @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "used by nflow-rest")
  public static class State extends ModelObject implements Comparable<State> {

    public State() {
      // default constructor is required by Jackson deserializer
    }

    public State(String id, String type, String description) {
      this.id = id;
      this.type = type;
      this.description = description;
    }

    public String id;
    public String type;
    public String description;
    public List<String> transitions = new ArrayList<>();
    public String onFailure;

    @Override
    @SuppressFBWarnings(value = "EQ_COMPARETO_USE_OBJECT_EQUALS", justification = "This class has a natural ordering that is inconsistent with equals")
    public int compareTo(State state) {
      return type.compareTo(state.type);
    }
  }

  public static class Signal extends ModelObject implements Comparable<Signal> {
    public Integer value;
    public String description;

    @Override
    @SuppressFBWarnings(value = "EQ_COMPARETO_USE_OBJECT_EQUALS", justification = "This class has a natural ordering that is inconsistent with equals")
    public int compareTo(Signal o) {
      return value.compareTo(o.value);
    }
  }

}

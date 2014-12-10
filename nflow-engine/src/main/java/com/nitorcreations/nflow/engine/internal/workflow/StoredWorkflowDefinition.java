package com.nitorcreations.nflow.engine.internal.workflow;

import java.util.ArrayList;
import java.util.List;

public class StoredWorkflowDefinition {
  public String type;
  public String description;
  public String onError;
  public List<State> states;

  public static class State implements Comparable<State> {

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
    public int compareTo(State state) {
      return type.compareTo(state.type);
    }
  }
}

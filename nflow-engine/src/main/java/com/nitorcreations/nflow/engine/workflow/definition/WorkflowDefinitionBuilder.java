package com.nitorcreations.nflow.engine.workflow.definition;

import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.start;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.nitorcreations.nflow.engine.internal.workflow.WorkflowDefinitionScanner;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowStateMethod;
import com.nitorcreations.nflow.engine.workflow.definition.DynamicWorkflowDefinition.GenericMethodState;

/**
 * Builder for dynamic workflow definitions.
 */
public class WorkflowDefinitionBuilder {
  final String type;
  WorkflowSettings.Builder settings = new WorkflowSettings.Builder();
  WorkflowState errorState;
  WorkflowState initialState;
  final Set<WorkflowState> states = new HashSet<>();
  final Map<String, WorkflowStateMethod> methods = new HashMap<>();
  final Map<Class<?>, Map<String, WorkflowStateMethod>> scannedMethods = new HashMap<>();

  /**
   * Create a new builder for a given workflow definition type.
   * @param type The workflow type.
   */
  public WorkflowDefinitionBuilder(String type) {
    this.type = type;
  }

  /**
   * Return the workflow settings builder.
   * @return Workflow settings builder.
   */
  public WorkflowSettings.Builder settings() {
    return settings;
  }

  /**
   * Adds a normal state to the workflow.
   * @param clazz The class that has the method that implements the state processing.
   * @param method The method that implements the state processing.
   * @param name The name of the state.
   * @return this.
   */
  public WorkflowDefinitionBuilder addState(Class<?> clazz, String method, String name) {
    return addState(clazz, method, normal, name, null);
  }

  /**
   * Set the initial state of the workflow.
   * @param clazz The class that has the method that implements the state processing.
   * @param method The method that implements the state processing.
   * @param name The name of the state.
   * @return this.
   */
  public WorkflowDefinitionBuilder setInitialState(Class<?> clazz, String method, String name) {
    initialState = state(start, name, null);
    add(clazz, method, initialState);
    return this;
  }

  private void add(Class<?> clazz, String method, WorkflowState state) {
    states.add(state);
    if (clazz == null) {
      return;
    }
    Map<String, WorkflowStateMethod> stateMethods = scannedMethods.get(clazz);
    if (stateMethods == null) {
      stateMethods = new WorkflowDefinitionScanner().getStateMethods(clazz);
      scannedMethods.put(clazz, stateMethods);
    }
    WorkflowStateMethod stateMethod = stateMethods.get(method);
    if (stateMethod == null) {
      throw new IllegalArgumentException("Could not find method " + method + " in class " + clazz.getName());
    }
    methods.put(state.name(), stateMethod);
  }

  /**
   * Sets the error state of the workflow. Use this when the error state has no processing method.
   * @param type The state type of the error state.
   * @param name The name of the state.
   * @return this.
   */
  public WorkflowDefinitionBuilder setErrorState(WorkflowStateType type, String name) {
    return setErrorState(null, null, type, name);
  }

  /**
   * Sets the error state of the workflow. Use this when the error state has a processing method.
   * @param clazz The class that has the method that implements the state processing.
   * @param method The method that implements the state processing.
   * @param type The state type of the error state.
   * @param name The name of the state.
   * @return this.
   */
  public WorkflowDefinitionBuilder setErrorState(Class<?> clazz, String method, WorkflowStateType type, String name) {
    errorState = state(type, name, null);
    add(clazz, method, errorState);
    return this;
  }

  /**
   * Adds a normal state to the workflow.
   * @param clazz The class that has the method that implements the state processing.
   * @param method The method that implements the state processing.
   * @param name The name of the state.
   * @param description The description of the state.
   * @return this.
   */
  public WorkflowDefinitionBuilder addState(Class<?> clazz, String method, WorkflowStateType stateType, String name,
      String description) {
    add(clazz, method, state(stateType, name, description));
    return this;
  }

  private WorkflowState state(WorkflowStateType stateType, String name, String description) {
    return new GenericMethodState(stateType, name, description);
  }

  /**
   * Creates the dynamic workflow definition.
   * @return a new workflow definition.
   */
  public AbstractWorkflowDefinition<WorkflowState> build() {
    return new DynamicWorkflowDefinition(this);
  }
}

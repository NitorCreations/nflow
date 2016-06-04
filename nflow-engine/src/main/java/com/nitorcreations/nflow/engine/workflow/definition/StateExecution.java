package com.nitorcreations.nflow.engine.workflow.definition;

import java.util.List;

import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;

/**
 * Provides access to workflow instance information.
 *
 * Variables are persisted after processing the state handler method.
 */
public interface StateExecution {

  /**
   * Return the id of the workflow instance.
   *
   * @return The workflow instance id.
   */
  int getWorkflowInstanceId();

  /**
   * Return the business key associated to the workflow instance.
   *
   * @return The business key.
   */
  String getBusinessKey();

  /**
   * Return the number of retry attempts in the current state.
   *
   * @return Number of retries. Zero when the state is executed for the first
   *         time. Increased by one every time the same state is executed again.
   */
  int getRetries();

  /**
   * Return a string value of the given variable.
   *
   * @param name The name of the variable.
   * @return The string value of the variable, or null if the variable does not exist.
   */
  String getVariable(String name);

  /**
   * Return the value of the given variable. The value is deserialized by the object mapper.
   *
   * @param name The name of the variable.
   * @param type The class of the variable.
   * @param <T> The type of object to be deserialized.
   * @return The deserialized value of class {code T}, or null if the variable does not exist.
   */
  <T> T getVariable(String name, Class<T> type);

  /**
   * Return the string value of the given variable, or {code defaultValue} if
   * the variable does not exist.
   *
   * @param name The name of the variable.
   * @param defaultValue The default value if the variable does not exist.
   * @return The string value of the variable or the default value.
   */
  String getVariable(String name, String defaultValue);

  /**
   * Return the value of the given variable, or {code defaultValue} if the variable does not
   * exist. The value is deserialized by the object mapper.
   *
   * @param name The name of the variable.
   * @param type The class of the variable.
   * @param defaultValue The default value if the variable does not exist.
   * @param <T> The type of object to be deserialized.
   * @return The deserialized value of class {code T}.
   */
  <T> T getVariable(String name, Class<T> type, T defaultValue);

  /**
   * Set the string value of the given variable.
   * @param name The name of the variable.
   * @param value The string value for the varible.
   */
  void setVariable(String name, String value);

  /**
   * Set the value for the given varible. The value must be serializable by the object mapper.
   * @param name The name of the variable.
   * @param value The value for the variable.
   */
  void setVariable(String name, Object value);

  /**
   * Return the external id of the workflow instance.
   *
   * @return The external id of the workflow instance.
   */
  String getWorkflowInstanceExternalId();

  /**
   * Add new child workflows. Child workflows are stored to database after current
   * state method processing completes successfully.
   * Note that child workflows are not visible to queryChildWorkflows() method before they are stored to database.
   * @param childWorkflows Child workflows to create.
   */
  void addChildWorkflows(WorkflowInstance... childWorkflows);

  /**
   * Add new workflows. Workflows are stored to database after current
   * state method processing completes successfully.
   * @param workflows Workflows to create.
   */
  void addWorkflows(WorkflowInstance... workflows);

  /**
   * Return child workflow instances for current workflow. Query is restricted to childs of current workflow.
   * @param query The query criterias.
   * @return List of child workflows that match the query.
   */
  List<WorkflowInstance> queryChildWorkflows(QueryWorkflowInstances query);

  /**
   * Return all child workflows for current workflow.
   * @return List of all child workflows.
   */
  List<WorkflowInstance> getAllChildWorkflows();

  /**
   * Notify parent workflow that it may start processing again. Calling this schedules parent workflow for immediate
   * execution. Scheduling is performed when current state method processing completes successfully.
   *
   * @param expectedStates If parent state is not one of the expected states, it is not woken up. If no expected states are
   * given, parent workflow is woken up regardless of the state.
   */
  void wakeUpParentWorkflow(String... expectedStates);

  /**
   * Create a builder for creating child workflows. Created builder has nextActivation set to current time.
   * @return Builder for creating child workflows.
   */
  WorkflowInstance.Builder workflowInstanceBuilder();

  /**
   * Control if action is created when workflow instance is update to the database after state processing. By default the action
   * is created. Additionally, the action is always created when new workflows or child workflows are created or when the state
   * variables are updated, regardless of this setting.
   * @param createAction Whether action should be created or not.
   */
  void setCreateAction(boolean createAction);

}

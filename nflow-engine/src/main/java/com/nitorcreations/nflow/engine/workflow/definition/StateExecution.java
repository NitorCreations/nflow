package com.nitorcreations.nflow.engine.workflow.definition;

/**
 * Provides access to workflow instance information.
 *
 * Variables are persisted after processing the state handler method.
 */
public interface StateExecution {

  /**
   * Return the id of the workflow instance.
   *
   * @return
   */
  int getWorkflowId();

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
   * @param name
   *          The name of the variable.
   * @return The string value of the variable.
   */
  String getVariable(String name);

  /**
   * Return the value of the given variable. The value is deserialized by the object mapper.
   *
   * @param name
   *          The name of the variable.
   * @param type
   *          The class of the variable.
   * @param <T>
   *          The type of object to be deserialized.
   * @return The deserialized value of class {code T}.
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
}

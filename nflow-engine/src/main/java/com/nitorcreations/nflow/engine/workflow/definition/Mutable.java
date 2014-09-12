package com.nitorcreations.nflow.engine.workflow.definition;

/**
 * Wrapper class to provide mutable object for immutable value.
 *
 * @param <T> The class of object to be wrapped.
 */
public class Mutable<T> {

  /**
   * The wrapped value.
   */
  public T val;

  /**
   * Default constructor.
   */
  public Mutable() {}

  /**
   * Creates a wrapper for {@code val}.
   *
   * @param val Any object to be wrapped.
   */
  public Mutable(T val) {
    this.val = val;
  }

  /**
   * Sets the new value for {@code val}.
   *
   * @param val New value.
   */
  public void setVal(T val) {
    this.val = val;
  }

  /**
   * Returns the wrapped value.
   *
   * @return Wrapped value.
   */
  public T getVal() {
    return val;
  }

  /**
   * Returns the string representation of the {@code val}.
   *
   * @return String representation.
   */
  @Override
  public String toString() {
    return String.valueOf(val);
  }
}

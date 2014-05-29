package com.nitorcreations.nflow.engine.workflow;

public class Mutable<T> {
  public T val;
  public Mutable() {}
  public Mutable(T val) {
    this.val = val;
  }
  public void setVal(T val) {
    this.val = val;
  }
  public T getVal() {
    return val;
  }
}

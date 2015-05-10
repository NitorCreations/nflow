package com.nitorcreations.nflow.engine.internal.storage.db;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

public interface SQLVariants {
  String currentTimePlusSeconds(int seconds);

  boolean hasUpdateReturning();

  String workflowStatus(WorkflowInstanceStatus status);

  String workflowStatus();

  String actionType();

  boolean hasUpdateableCTE();

  String least(String value1, String value2);

  String nextActivationUpdate();

  String castToText();
}

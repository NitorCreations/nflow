package com.nitorcreations.nflow.engine.internal.storage.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

public interface SQLVariants {
  String currentTimePlusSeconds(int seconds);

  boolean hasUpdateReturning();

  String workflowStatus(WorkflowInstanceStatus status);

  String workflowStatus();

  String actionType();

  boolean hasUpdateableCTE();

  String nextActivationUpdate();

  String castToText();

  String limit(String query, String limit);

  int textType();

  void setText(PreparedStatement ps, int parameterIndex, String value) throws SQLException;

  String getText(ResultSet rs, int columnIndex) throws SQLException;
}

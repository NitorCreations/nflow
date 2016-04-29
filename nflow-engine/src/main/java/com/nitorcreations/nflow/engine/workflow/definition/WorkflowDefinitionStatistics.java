package com.nitorcreations.nflow.engine.workflow.definition;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "used by nflow-rest")
public class WorkflowDefinitionStatistics {
  public long allInstances;
  public long queuedInstances;
}

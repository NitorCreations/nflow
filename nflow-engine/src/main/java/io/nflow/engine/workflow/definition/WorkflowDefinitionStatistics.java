package io.nflow.engine.workflow.definition;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;

@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "used by nflow-rest")
public class WorkflowDefinitionStatistics extends ModelObject {
  public long allInstances;
  public long queuedInstances;
}

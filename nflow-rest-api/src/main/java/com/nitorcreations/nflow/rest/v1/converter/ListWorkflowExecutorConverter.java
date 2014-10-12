package com.nitorcreations.nflow.rest.v1.converter;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.workflow.executor.WorkflowExecutor;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowExecutorResponse;

@Component
public class ListWorkflowExecutorConverter {

  public ListWorkflowExecutorResponse convert(WorkflowExecutor executor) {
    ListWorkflowExecutorResponse resp = new ListWorkflowExecutorResponse();
    resp.id = executor.id;
    resp.host = executor.host;
    resp.pid = executor.pid;
    resp.executorGroup = executor.executorGroup;
    resp.started = executor.started;
    resp.active = executor.active;
    resp.expires = executor.expires;
    return resp;
  }
}

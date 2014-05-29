package com.nitorcreations.nflow.engine.workflow.data;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.engine.domain.StateExecutionImpl;
import com.nitorcreations.nflow.engine.workflow.data.WorkflowStateMethod.StateParameter;

@Component
public class ObjectStringMapper {
  private final ObjectMapper mapper;

  @Inject
  public ObjectStringMapper(@Named("nflow-ObjectMapper") ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public Object[] createArguments(StateExecutionImpl execution,
      WorkflowStateMethod method) {
    Object[] args = new Object[method.params.length + 1];
    args[0] = execution;
    StateParameter[] params = method.params;
    for (int i = 0; i < params.length; i++) {
      StateParameter param = params[i];
      String value = execution.getVariable(param.key);
      if (value == null) {
        args[i + 1] = param.nullValue;
        continue;
      }
      if (String.class.equals(param.type)) {
        args[i + 1] = value;
        continue;
      }
      JavaType type = mapper.getTypeFactory().constructType(param.type);
      try {
        args[i + 1] = mapper.readValue(value, type);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return args;
  }

  public void storeArguments(StateExecutionImpl execution,
      WorkflowStateMethod method, Object[] args) {
    StateParameter[] params = method.params;
    for (int i = 0; i < params.length; i++) {
      StateParameter param = params[i];
      if (param.readoOnly) {
        continue;
      }
      Object value = args[i + 1];
      if (value == null) {
        continue;
      }
      try {
        String sVal = mapper.writeValueAsString(value);
        execution.setVariable(param.key, sVal);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }

}

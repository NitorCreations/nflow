package io.nflow.engine.internal.workflow;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import io.nflow.engine.config.EngineConfiguration.EngineObjectMapperSupplier;
import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.workflow.WorkflowStateMethod.StateParameter;
import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.StateExecution;

@Component
public class ObjectStringMapper {
  private final ObjectMapper mapper;

  @Inject
  public ObjectStringMapper(@NFlow EngineObjectMapperSupplier nflowObjectMapper) {
    this.mapper = nflowObjectMapper.get();
  }

  @SuppressWarnings("unchecked")
  @SuppressFBWarnings(value = "UCC_UNRELATED_COLLECTION_CONTENTS", justification = "args are unrelated")
  public Object[] createArguments(StateExecution execution,
      WorkflowStateMethod method) {
    Object[] args = new Object[method.params.length + 1];
    args[0] = execution;
    StateParameter[] params = method.params;
    for (int i = 1; i <= params.length; i++) {
      StateParameter param = params[i - 1];
      String value = execution.getVariable(param.key);
      if (value == null) {
        Object def = param.nullValue;
        if (def instanceof Constructor) {
          try {
            def = ((Constructor<Object>) def).newInstance();
          } catch (InstantiationException | IllegalAccessException
              | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate default value for " + param.key, e);
          }
        }
        args[i] = def;
      } else if (String.class.equals(param.type)) {
        args[i] = value;
      } else {
        args[i] = convertToObject(param.type, param.key, value);
      }
      if (param.mutable) {
        args[i] = new Mutable<>(args[i]);
      }
    }
    return args;
  }

  public Object convertToObject(Type type, String key, String value) {
    JavaType javaType = mapper.getTypeFactory().constructType(type);
    try {
      return mapper.readValue(value, javaType);
    } catch (IOException e) {
      throw new RuntimeException("Failed to deserialize value for " + key, e);
    }
  }

  @SuppressWarnings("unchecked")
  public void storeArguments(StateExecution execution,
      WorkflowStateMethod method, Object[] args) {
    StateParameter[] params = method.params;
    for (int i = 0; i < params.length; i++) {
      StateParameter param = params[i];
      if (param.readOnly) {
        continue;
      }
      Object value = args[i + 1];
      if (value == null) {
        continue;
      }
      String sVal;
      if (param.mutable) {
        value = ((Mutable<Object>) value).val;
        if (value == null) {
          continue;
        }
      }
      if (String.class.equals(param.type)) {
        sVal = (String) value;
      } else {
        sVal = convertFromObject(param.key, value);
      }
      execution.setVariable(param.key, sVal);
    }
  }

  public String convertFromObject(String key, Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize value for " + key, e);
    }
  }

}

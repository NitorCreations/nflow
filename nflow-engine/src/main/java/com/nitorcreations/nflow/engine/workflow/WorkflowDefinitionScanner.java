package com.nitorcreations.nflow.engine.workflow;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;

import com.nitorcreations.nflow.engine.workflow.WorkflowStateMethod.StateParameter;

public class WorkflowDefinitionScanner {
  public Map<String, WorkflowStateMethod> getStateMethods(@SuppressWarnings("rawtypes") Class<? extends WorkflowDefinition> definition) {
    final Map<String, WorkflowStateMethod> methods = new HashMap<>();
    ReflectionUtils.doWithMethods(definition, new MethodCallback() {
      @Override
      public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
        List<StateParameter> params = new ArrayList<>();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 1; i < genericParameterTypes.length; ++i) {
          for (Annotation a : parameterAnnotations[i]) {
            if (Data.class.equals(a.annotationType())) {
              params.add(new StateParameter(((Data) a).value(), genericParameterTypes[i]));
              break;
            }
          }
        }
        if (params.size() != genericParameterTypes.length - 1) {
          throw new IllegalStateException("Not all parameter names could be resolved for " + method + ". Maybe missing @Data annotation?");
        }
        methods.put(method.getName(), new WorkflowStateMethod(method, params.toArray(new StateParameter[params.size()])));
      }
    }, new WorkflowTransitionMethod());
    return methods;
  }

  static final class WorkflowTransitionMethod implements MethodFilter {
    @Override
    public boolean matches(Method method) {
      int mod = method.getModifiers();
      Class<?>[] parameterTypes = method.getParameterTypes();
      return isPublic(mod) && !isStatic(mod) && parameterTypes.length >= 1 && StateExecution.class.equals(parameterTypes[0]);
    }
  }
}
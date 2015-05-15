package com.nitorcreations.nflow.engine.internal.workflow;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ReflectionUtils.doWithMethods;
import static org.springframework.util.ReflectionUtils.findMethod;
import static org.springframework.util.ReflectionUtils.invokeMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;

import com.nitorcreations.nflow.engine.internal.workflow.WorkflowStateMethod.StateParameter;
import com.nitorcreations.nflow.engine.workflow.definition.Mutable;
import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.StateVar;

public class WorkflowDefinitionScanner {

  private static final Logger logger = getLogger(WorkflowDefinitionScanner.class);

  private static final Set<Type> knownImmutableTypes = new LinkedHashSet<>();
  {
    knownImmutableTypes.addAll(asList(Boolean.TYPE, Boolean.class, Byte.TYPE, Byte.class, Character.TYPE, Character.class, Short.TYPE, Short.class, Integer.TYPE, Integer.class, Long.TYPE, Long.class, Float.TYPE, Float.class, Double.TYPE, Double.class, String.class, BigDecimal.class, BigInteger.class, Enum.class));
  }

  public Map<String, WorkflowStateMethod> getStateMethods(Class<?> definition) {
    final Map<String, WorkflowStateMethod> methods = new LinkedHashMap<>();
    doWithMethods(definition, new MethodCallback() {
      @Override
      public void doWith(Method method) throws IllegalArgumentException {
        List<StateParameter> params = new ArrayList<>();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 1; i < genericParameterTypes.length; ++i) {
          for (Annotation a : parameterAnnotations[i]) {
            if (StateVar.class.equals(a.annotationType())) {
              StateVar stateInfo = (StateVar) a;
              Type type = genericParameterTypes[i];
              Class<?> clazz = parameterTypes[i];
              boolean mutable = false;
              boolean readOnly = isReadOnly(type);
              if (Mutable.class.isAssignableFrom(clazz)) {
                ParameterizedType pType = (ParameterizedType) type;
                type = pType.getActualTypeArguments()[0];
                readOnly = false;
                mutable = true;
              }
              params.add(new StateParameter(stateInfo.value(), type, defaultValue(stateInfo, clazz), stateInfo.readOnly() || readOnly, mutable));
              break;
            }
          }
        }
        if (params.size() != genericParameterTypes.length - 1) {
          throw new IllegalStateException("Not all parameter names could be resolved for " + method + ". Maybe missing @StateVar annotation?");
        }
        if (methods.containsKey(method.getName())) {
          throw new IllegalStateException("Method " + method + " was overloaded. Overloading state methods is not allowed.");
        }
        methods.put(method.getName(), new WorkflowStateMethod(method, params.toArray(new StateParameter[params.size()])));
      }
    }, new WorkflowTransitionMethod());
    return methods;
  }

  boolean isReadOnly(Type type) {
    return knownImmutableTypes.contains(type);
  }

  Object defaultValue(StateVar stateInfo, Class<?> clazz) {
    if (clazz.isPrimitive()) {
      return invokeMethod(findMethod(primitiveToWrapper(clazz), "valueOf", String.class), null, "0");
    }
    if (stateInfo != null && stateInfo.instantiateIfNotExists()) {
      try {
        Constructor<?> ctr = clazz.getConstructor();
        ctr.newInstance();
        return ctr;
      } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        logger.warn("Could not instantiate " + clazz + " using empty constructor", e);
      }
    }
    return null;
  }

  static final class WorkflowTransitionMethod implements MethodFilter {
    @Override
    public boolean matches(Method method) {
      int mod = method.getModifiers();
      Class<?>[] parameterTypes = method.getParameterTypes();
      return isPublic(mod) && !isStatic(mod) && hasStateExecutionParameter(parameterTypes) &&
          hasValidReturnType(method.getReturnType());
    }

    private boolean hasValidReturnType(Class<?> returnType) {
      return NextAction.class.equals(returnType) || Void.TYPE.equals(returnType);
    }

    private boolean hasStateExecutionParameter(Class<?>[] parameterTypes) {
      return parameterTypes.length >= 1 && StateExecution.class.equals(parameterTypes[0]);
    }
  }
}
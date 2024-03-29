package io.nflow.engine.internal.workflow;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ReflectionUtils.doWithFields;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.springframework.util.ReflectionUtils.FieldFilter;
import org.springframework.util.ReflectionUtils.MethodFilter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.workflow.WorkflowStateMethod.StateParameter;
import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;

public class WorkflowDefinitionScanner {

  private static final Logger logger = getLogger(WorkflowDefinitionScanner.class);

  private static final Set<Class<?>> boxedPrimitiveTypes = Stream
      .of(Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class)
      .collect(collectingAndThen(toCollection(LinkedHashSet::new), Collections::unmodifiableSet));

  private static final Set<Type> knownImmutableTypes = Stream
      .of(Boolean.TYPE, Boolean.class, Byte.TYPE, Byte.class, Character.TYPE, Character.class, Short.TYPE, Short.class,
          Integer.TYPE, Integer.class, Long.TYPE, Long.class, Float.TYPE, Float.class, Double.TYPE, Double.class, String.class,
          BigDecimal.class, BigInteger.class, Enum.class)
      .collect(collectingAndThen(toCollection(LinkedHashSet::new), Collections::unmodifiableSet));

  public Map<String, WorkflowStateMethod> getStateMethods(Class<?> definition) {
    final Map<String, WorkflowStateMethod> methods = new LinkedHashMap<>();
    doWithMethods(definition, method -> {
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
              if (type instanceof ParameterizedType) {
                type = ((ParameterizedType) type).getRawType();
              }
              clazz = (Class<?>) type;
              readOnly = false;
              mutable = true;
            }
            params.add(new StateParameter(stateInfo.value(), type, defaultValue(stateInfo, clazz),
                readOnly || stateInfo.readOnly(), mutable));
            break;
          }
        }
      }
      if (params.size() != genericParameterTypes.length - 1) {
        throw new IllegalStateException(
            "Not all parameter names could be resolved for " + method + ". Maybe missing @StateVar annotation?");
      }
      assertNoDuplicates(method, params);
      if (methods.containsKey(method.getName())) {
        throw new IllegalStateException("Method " + method + " was overloaded. Overloading state methods is not allowed.");
      }
      methods.put(method.getName(), new WorkflowStateMethod(method, params.toArray(new StateParameter[params.size()])));
    }, new WorkflowTransitionMethod());
    return methods;
  }

  private void assertNoDuplicates(Method method, Collection<StateParameter> params) {
    Set<String> names = new HashSet<>(params.size(), 1);
    for (StateParameter p : params) {
      if (!names.add(p.key)) {
        throw new IllegalStateException("Method " + method + " had two state variables with same name: " + p.key + ".");
      }
    }
  }

  public Set<WorkflowState> getPublicStaticWorkflowStates(Class<? extends WorkflowDefinition> definition) {
    Set<WorkflowState> states = new HashSet<>();
    doWithFields(definition, field -> states.add((WorkflowState) field.get(null)), isPublicStaticWorkflowState());
    return states;
  }

  private FieldFilter isPublicStaticWorkflowState() {
    return field -> {
      var modifiers = field.getModifiers();
      return isPublic(modifiers) && isStatic(modifiers) && WorkflowState.class.isAssignableFrom(field.getType());
    };
  }

  boolean isReadOnly(Type type) {
    return knownImmutableTypes.contains(type);
  }

  @SuppressFBWarnings(value = { "URV_UNRELATED_RETURN_VALUES", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE" },
      justification = "return values are unrelated, npe is unlikely")
  Object defaultValue(StateVar stateInfo, Class<?> clazz) {
    if (clazz == char.class || clazz == Character.class) {
      return Character.valueOf((char) 0);
    }
    if (clazz.isPrimitive() || boxedPrimitiveTypes.contains(clazz)) {
      return invokeMethod(findMethod(primitiveToWrapper(clazz), "valueOf", String.class), null, "0");
    }
    if (stateInfo != null && stateInfo.instantiateIfNotExists()) {
      if (clazz.isInterface()) {
        if (Map.class.isAssignableFrom((clazz))) {
          clazz = HashMap.class;
        } else if (Collections.class.isAssignableFrom((clazz)) || List.class.isAssignableFrom((clazz))) {
          clazz = ArrayList.class;
        }
      }
      try {
        Constructor<?> ctr = clazz.getConstructor();
        ctr.newInstance();
        return ctr;
      } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
          | IllegalArgumentException | InvocationTargetException e) {
        logger.warn("Could not instantiate {} using empty constructor", clazz, e);
      }
    }
    return null;
  }

  static final class WorkflowTransitionMethod implements MethodFilter {
    @Override
    public boolean matches(Method method) {
      int mod = method.getModifiers();
      Class<?>[] parameterTypes = method.getParameterTypes();
      return isPublic(mod) && !isStatic(mod) && hasStateExecutionParameter(parameterTypes)
          && hasValidReturnType(method.getReturnType());
    }

    private boolean hasValidReturnType(Class<?> returnType) {
      return NextAction.class.equals(returnType) || Void.TYPE.equals(returnType);
    }

    private boolean hasStateExecutionParameter(Class<?>... parameterTypes) {
      return parameterTypes.length >= 1 && StateExecution.class.equals(parameterTypes[0]);
    }
  }
}

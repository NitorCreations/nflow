package io.nflow.engine.internal.workflow;

import static io.nflow.engine.workflow.definition.TestState.BEGIN;
import static io.nflow.engine.workflow.definition.TestState.DONE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import org.hamcrest.CustomMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.nflow.engine.internal.workflow.WorkflowStateMethod.StateParameter;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;

@SuppressWarnings("unused")
public class WorkflowDefinitionScannerTest {
  WorkflowDefinitionScanner scanner;

  @BeforeEach
  public void setup() {
    scanner = new WorkflowDefinitionScanner();
  }

  @Test
  public void overloadingStateMethodShouldThrowException() {
    IllegalStateException thrown = assertThrows(IllegalStateException.class,
            () -> scanner.getStateMethods(OverloadedStateMethodWorkflow.class));
    assertThat(thrown.getMessage(), containsString("OverloadedStateMethodWorkflow.end"));
    assertThat(thrown.getMessage(), containsString("Overloading state methods is not allowed."));
  }

  @Test
  public void missingStateVarAnnotationShouldThrowException() {
    IllegalStateException thrown = assertThrows(IllegalStateException.class,
            () -> scanner.getStateMethods(MissingStateVarWorkflow.class));
    assertThat(thrown.getMessage(), containsString("MissingStateVarWorkflow.end"));
    assertThat(thrown.getMessage(), containsString("missing @StateVar annotation"));
  }

  @Test
  public void unknownAnnotationOnParameterIsIgnored() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(UnknownAnnotationWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertThat(methods.get("end").params[0], stateParam("paramKey", String.class, true, false));
  }

  @Test
  public void mutableStateParamSetsMutableFlag() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(MutableParamWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertThat(methods.get("end").params[0], stateParam("paramKey", String.class, false, true));
    assertThat(methods.get("end").params[0].nullValue, nullValue());
    assertThat(methods.get("end").params[1], stateParam("longKey", Long.class, false, true));
    assertThat(methods.get("end").params[1].nullValue, is(0L));
  }

  @Test
  public void instantiateNullFlagCausesParameterObjectToInstantiate() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(InitiateParameterWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramKey", ParamObj.class, false, false));

    StateParameter longParam = methods.get("end").params[1];
    assertThat(longParam, stateParam("paramKey2", long.class, true, false));
    assertEquals(0L, longParam.nullValue);
  }

  @Test
  public void instantiateWithBoolean() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(BooleanObjectWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", boolean.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Boolean.class, true, false));
    assertEquals(Boolean.FALSE, param.nullValue);
  }

  @Test
  public void instantiateWithByte() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(ByteObjectWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", byte.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Byte.class, true, false));
    assertEquals((byte)0, param.nullValue);
  }

  @Test
  public void instantiateWithCharacter() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(CharacterObjectWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", char.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Character.class, true, false));
    assertEquals((char)0, param.nullValue);
  }

  @Test
  public void instantiateWithShort() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(ShortObjectWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", short.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Short.class, true, false));
    assertEquals(Short.valueOf((short)0), param.nullValue);
  }

  @Test
  public void instantiateWithInteger() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(IntegerObjectWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", int.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Integer.class, true, false));
    assertEquals(Integer.valueOf(0), param.nullValue);
  }

  @Test
  public void instantiateWithLong() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(LongObjectWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", long.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Long.class, true, false));
    assertEquals(Long.valueOf(0), param.nullValue);
  }

  @Test
  public void instantiateWithFloat() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(FloatObjectWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", float.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Float.class, true, false));
    assertEquals(Float.valueOf(0), param.nullValue);
  }

  @Test
  public void instantiateWithDouble() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(DoubleObjectWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", double.class, true, false));

    StateParameter longParam = methods.get("end").params[1];
    assertThat(longParam, stateParam("paramBoxed", Double.class, true, false));
    assertEquals(Double.valueOf(0), longParam.nullValue);
  }

  @Test
  public void onlyPublicMethodsWithCorrectSignatureAreReturned() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(NonStateMethodsWorkflow.class);
    assertThat(methods, allOf(hasKey("start"), hasKey("end"), hasKey("doesNotReturnNextState")));
    assertThat(methods.keySet().size(), is(3));
  }

  @Test
  public void readOnlyStateVarFlagSetsFlagInStateParameter() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(ReadOnlyStateVarWorkflow.class);
    assertThat(methods, both(hasKey("start")).and(hasKey("end")));
    assertThat(methods.get("end").params[0], stateParam("paramKey", String.class, true, false));
  }

  private CustomMatcher<StateParameter> stateParam(final String key, final Type type, final boolean readOnly,
      final boolean mutable) {
    return new CustomMatcher<StateParameter>("") {
      @Override
      public boolean matches(Object item) {
        StateParameter p = (StateParameter) item;
        return Objects.equals(key, p.key) //
            && Objects.equals(type, p.type) //
            && Objects.equals(readOnly, p.readOnly) //
            && Objects.equals(mutable, p.mutable);
      }
    };
  }

  @Retention(RUNTIME)
  @Target(PARAMETER)
  public @interface Dummy{
  }

  public static class ParamObj { }

  public static class OverloadedStateMethodWorkflow extends AbstractWorkflowDefinition {
    public OverloadedStateMethodWorkflow() {
      super("overload", BEGIN, DONE);
    }

    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec, @StateVar("foo") String param) { return null; }
  }

  public static class MissingStateVarWorkflow extends AbstractWorkflowDefinition {
    public MissingStateVarWorkflow() {
      super("missingStateVar", BEGIN, DONE);
    }

    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec, String param) { return null; }
  }

  public static class UnknownAnnotationWorkflow extends AbstractWorkflowDefinition {
    public UnknownAnnotationWorkflow() {
      super("unknownAnnotation", BEGIN, DONE);
    }

    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec, @Dummy @StateVar("paramKey") String param) { return null; }
  }

  public static class MutableParamWorkflow extends AbstractWorkflowDefinition {
    public MutableParamWorkflow() {
      super("mutableParam", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec, @StateVar("paramKey") Mutable<String> param, @StateVar(value="longKey", instantiateIfNotExists=true) Mutable<Long> param2) { return null; }
  }

  public static class InitiateParameterWorkflow extends AbstractWorkflowDefinition {
    public InitiateParameterWorkflow() {
      super("instantiateNull", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
        @StateVar(value = "paramKey", instantiateIfNotExists = true) ParamObj param,
        @StateVar(value = "paramKey2", instantiateIfNotExists = true) long paramPrimitive) { return null; }
  }

  public static class BooleanObjectWorkflow extends AbstractWorkflowDefinition {
    public BooleanObjectWorkflow() {
      super("instantiateNull", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) boolean paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Boolean paramBoxed) { return null; }
  }

  public static class ByteObjectWorkflow extends AbstractWorkflowDefinition {
    public ByteObjectWorkflow() {
      super("instantiateNull", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) byte paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Byte paramBoxed) { return null; }
  }

  public static class CharacterObjectWorkflow extends AbstractWorkflowDefinition {
    public CharacterObjectWorkflow() {
      super("instantiateNull", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) char paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Character paramBoxed) { return null; }
  }

  public static class ShortObjectWorkflow extends AbstractWorkflowDefinition {
    public ShortObjectWorkflow() {
      super("instantiateNull", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) short paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Short paramBoxed) { return null; }
  }

  public static class IntegerObjectWorkflow extends AbstractWorkflowDefinition {
    public IntegerObjectWorkflow() {
      super("instantiateNull", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) int paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Integer paramBoxed) { return null; }
  }

  public static class LongObjectWorkflow extends AbstractWorkflowDefinition {
    public LongObjectWorkflow() {
      super("instantiateNull", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) long paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Long paramBoxed) { return null; }
  }

  public static class FloatObjectWorkflow extends AbstractWorkflowDefinition {
    public FloatObjectWorkflow() {
      super("instantiateNull", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) float paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Float paramBoxed) { return null; }
  }

  public static class DoubleObjectWorkflow extends AbstractWorkflowDefinition {
    public DoubleObjectWorkflow() {
      super("instantiateNull", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) double paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Double paramBoxed) { return null; }
  }

  public static class NonStateMethodsWorkflow extends AbstractWorkflowDefinition {
    public NonStateMethodsWorkflow() {
      super("nonStateMethods", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec) { return null; }
    public NextAction noArgs() { return null; }
    public NextAction wrongFirstArg(String x) { return null; }
    public void doesNotReturnNextState(StateExecution exec) { }
    public static NextAction staticMethod(StateExecution exec) { return null; }
    protected NextAction nonPublic(StateExecution exec) { return null; }
  }

  public static class ReadOnlyStateVarWorkflow extends AbstractWorkflowDefinition {
    public ReadOnlyStateVarWorkflow() {
      super("readOnly", BEGIN, DONE);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec, @StateVar(value = "paramKey", readOnly = true) String param) { return null; }
  }
}

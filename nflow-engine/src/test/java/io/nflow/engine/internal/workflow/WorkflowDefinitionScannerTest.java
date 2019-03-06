package io.nflow.engine.internal.workflow;

import static com.nitorcreations.Matchers.hasItemsOf;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import org.hamcrest.CustomMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.nflow.engine.internal.workflow.WorkflowStateMethod.StateParameter;
import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

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
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertThat(methods.get("end").params[0], stateParam("paramKey", String.class, true, false));
  }

  @Test
  public void mutableStateParamSetsMutableFlag() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(MutableParamWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertThat(methods.get("end").params[0], stateParam("paramKey", String.class, false, true));
  }

  @Test
  public void instantiateNullFlagCausesParameterObjectToInstantiate() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(InitiateParameterWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramKey", ParamObj.class, false, false));

    StateParameter longParam = methods.get("end").params[1];
    assertThat(longParam, stateParam("paramKey2", long.class, true, false));
    assertEquals(0L, longParam.nullValue);
  }

  @Test
  public void instantiateWithBoolean() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(BooleanObjectWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", boolean.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Boolean.class, true, false));
    assertEquals(Boolean.FALSE, param.nullValue);
  }

  @Test
  public void instantiateWithByte() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(ByteObjectWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", byte.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Byte.class, true, false));
    assertEquals((byte)0, param.nullValue);
  }

  @Test
  public void instantiateWithCharacter() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(CharacterObjectWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", char.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Character.class, true, false));
    assertEquals((char)0, param.nullValue);
  }

  @Test
  public void instantiateWithShort() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(ShortObjectWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", short.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Short.class, true, false));
    assertEquals(Short.valueOf((short)0), param.nullValue);
  }

  @Test
  public void instantiateWithInteger() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(IntegerObjectWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", int.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Integer.class, true, false));
    assertEquals(Integer.valueOf(0), param.nullValue);
  }

  @Test
  public void instantiateWithLong() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(LongObjectWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", long.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Long.class, true, false));
    assertEquals(Long.valueOf(0), param.nullValue);
  }

  @Test
  public void instantiateWithFloat() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(FloatObjectWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", float.class, true, false));

    StateParameter param = methods.get("end").params[1];
    assertThat(param, stateParam("paramBoxed", Float.class, true, false));
    assertEquals(Float.valueOf(0), param.nullValue);
  }

  @Test
  public void instantiateWithDouble() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(DoubleObjectWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertNotNull(methods.get("end").params[0].nullValue);
    assertThat(methods.get("end").params[0], stateParam("paramPrimitive", double.class, true, false));

    StateParameter longParam = methods.get("end").params[1];
    assertThat(longParam, stateParam("paramBoxed", Double.class, true, false));
    assertEquals(Double.valueOf(0), longParam.nullValue);
  }

  @Test
  public void onlyPublicMethodsWithCorrectSignatureAreReturned() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(NonStateMethodsWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end", "doesNotReturnNextState")));
    assertThat(methods.keySet().size(), is(3));
  }

  @Test
  public void readOnlyStateVarFlagSetsFlagInStateParameter() {
    Map<String, WorkflowStateMethod> methods = scanner.getStateMethods(ReadOnlyStateVarWorkflow.class);
    assertThat(methods.keySet(), hasItemsOf(asList("start", "end")));
    assertThat(methods.get("end").params[0], stateParam("paramKey", String.class, true, false));
  }

  private CustomMatcher<StateParameter> stateParam(final String key, final Type type, final boolean readOnly, final boolean mutable) {
    return new CustomMatcher<WorkflowStateMethod.StateParameter>("") {
      @Override
      public boolean matches(Object item) {
        StateParameter p = (StateParameter) item;
        return Objects.equals(key, p.key) && Objects.equals(type, p.type)
            && Objects.equals(readOnly, p.readOnly) && Objects.equals(mutable, p.mutable);

      }
    };
  }
  public static enum ScannerState implements WorkflowState{
    start(WorkflowStateType.start),
    end(WorkflowStateType.end);
    private final WorkflowStateType type;

    private ScannerState(WorkflowStateType type) {
      this.type = type;
    }
    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return null;
    }
  }

  @Retention(RUNTIME)
  @Target(PARAMETER)
  public @interface Dummy{
  }

  public static class ParamObj { }

  public static class OverloadedStateMethodWorkflow extends WorkflowDefinition<ScannerState> {
    public OverloadedStateMethodWorkflow() {
      super("overload", ScannerState.start, ScannerState.end);
    }

    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec, @StateVar("foo") String param) { return null; }
  }

  public static class MissingStateVarWorkflow extends WorkflowDefinition<ScannerState> {
    public MissingStateVarWorkflow() {
      super("missingStateVar", ScannerState.start, ScannerState.end);
    }

    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec, String param) { return null; }
  }

  public static class UnknownAnnotationWorkflow extends WorkflowDefinition<ScannerState> {
    public UnknownAnnotationWorkflow() {
      super("unknownAnnotation", ScannerState.start, ScannerState.end);
    }

    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec, @Dummy @StateVar("paramKey") String param) { return null; }
  }

  public static class MutableParamWorkflow extends WorkflowDefinition<ScannerState> {
    public MutableParamWorkflow() {
      super("mutableParam", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec, @StateVar("paramKey") Mutable<String> param) { return null; }
  }

  public static class InitiateParameterWorkflow extends WorkflowDefinition<ScannerState> {
    public InitiateParameterWorkflow() {
      super("instantiateNull", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
        @StateVar(value = "paramKey", instantiateIfNotExists = true) ParamObj param,
        @StateVar(value = "paramKey2", instantiateIfNotExists = true) long paramPrimitive) { return null; }
  }

  public static class BooleanObjectWorkflow extends WorkflowDefinition<ScannerState> {
    public BooleanObjectWorkflow() {
      super("instantiateNull", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) boolean paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Boolean paramBoxed) { return null; }
  }

  public static class ByteObjectWorkflow extends WorkflowDefinition<ScannerState> {
    public ByteObjectWorkflow() {
      super("instantiateNull", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) byte paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Byte paramBoxed) { return null; }
  }

  public static class CharacterObjectWorkflow extends WorkflowDefinition<ScannerState> {
    public CharacterObjectWorkflow() {
      super("instantiateNull", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) char paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Character paramBoxed) { return null; }
  }

  public static class ShortObjectWorkflow extends WorkflowDefinition<ScannerState> {
    public ShortObjectWorkflow() {
      super("instantiateNull", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) short paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Short paramBoxed) { return null; }
  }

  public static class IntegerObjectWorkflow extends WorkflowDefinition<ScannerState> {
    public IntegerObjectWorkflow() {
      super("instantiateNull", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) int paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Integer paramBoxed) { return null; }
  }

  public static class LongObjectWorkflow extends WorkflowDefinition<ScannerState> {
    public LongObjectWorkflow() {
      super("instantiateNull", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) long paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Long paramBoxed) { return null; }
  }

  public static class FloatObjectWorkflow extends WorkflowDefinition<ScannerState> {
    public FloatObjectWorkflow() {
      super("instantiateNull", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) float paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Float paramBoxed) { return null; }
  }

  public static class DoubleObjectWorkflow extends WorkflowDefinition<ScannerState> {
    public DoubleObjectWorkflow() {
      super("instantiateNull", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec,
                          @StateVar(value = "paramPrimitive", instantiateIfNotExists = true) double paramPrimitive,
                          @StateVar(value = "paramBoxed", instantiateIfNotExists = true) Double paramBoxed) { return null; }
  }

  public static class NonStateMethodsWorkflow extends WorkflowDefinition<ScannerState> {
    public NonStateMethodsWorkflow() {
      super("nonStateMethods", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec) { return null; }
    public NextAction noArgs() { return null; }
    public NextAction wrongFirstArg(String x) { return null; }
    public void doesNotReturnNextState(StateExecution exec) { }
    public static NextAction staticMethod(StateExecution exec) { return null; }
    protected NextAction nonPublic(StateExecution exec) { return null; }
  }

  public static class ReadOnlyStateVarWorkflow extends WorkflowDefinition<ScannerState> {
    public ReadOnlyStateVarWorkflow() {
      super("readOnly", ScannerState.start, ScannerState.end);
    }
    public NextAction start(StateExecution exec) { return null; }
    public NextAction end(StateExecution exec, @StateVar(value = "paramKey", readOnly = true) String param) { return null; }
  }
}

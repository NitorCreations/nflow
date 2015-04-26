package com.nitorcreations.nflow.engine.internal.workflow;

import static com.nitorcreations.Matchers.hasItemsOf;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import org.hamcrest.CustomMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.nitorcreations.nflow.engine.internal.workflow.WorkflowStateMethod.StateParameter;
import com.nitorcreations.nflow.engine.workflow.definition.Mutable;
import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.StateVar;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;


public class WorkflowDefinitionScannerTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  WorkflowDefinitionScanner scanner;

  @Before
  public void setup() {
    scanner = new WorkflowDefinitionScanner();
  }
  @Test
  public void overloadingStateMethodShouldThrowException() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("OverloadedStateMethodWorkflow.end");
    thrown.expectMessage("Overloading state methods is not allowed.");
    scanner.getStateMethods(OverloadedStateMethodWorkflow.class);
  }
  @Test
  public void missingStateVarAnnotationShouldThrowException() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("MissingStateVarWorkflow.end");
    thrown.expectMessage("missing @StateVar annotation");
    scanner.getStateMethods(MissingStateVarWorkflow.class);
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

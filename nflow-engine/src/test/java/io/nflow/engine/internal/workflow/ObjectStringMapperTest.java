package io.nflow.engine.internal.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import io.nflow.engine.internal.workflow.WorkflowStateMethod.StateParameter;
import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.StateExecution;

@ExtendWith(MockitoExtension.class)
class ObjectStringMapperTest {

  private final ObjectStringMapper mapper = new ObjectStringMapper(JsonMapper::new);

  @Mock
  StateExecution execution;

  @Test
  public void stringRoundTrip() {
    verifyRoundTrip(String.class, "test☃");
  }

  @Test
  public void longRoundTrip() {
    verifyRoundTrip(Long.class, -1L);
  }

  private void verifyRoundTrip(Type type, Object value) {
    String data = mapper.convertFromObject(null, value);
    Object result = mapper.convertToObject(type, null, data);
    assertThat(result, is(value));
  }

  @Test
  public void methodStringParam() {
    verifyParam(String.class, "test€", "test€");
  }

  @Test
  public void methodLongParam() {
    verifyParam(Long.class, "42", 42L);
  }

  @Test
  public void methodPrimitiveIntParam() {
    verifyParam(Integer.TYPE, "128", 0, 0, 128);
  }

  @Test
  public void methodMutableLongParam() {
    verifyParam(Long.class, "42", 0L, new Mutable<>(0L), new Mutable<>(42L));
    verifyParam(Long.class, "42", null, new Mutable<>(null), new Mutable<>(42L));
  }

  @Test
  public void storeArgumentsSetsOnlyWritableObjectVariablesThatAreNotExplicitlySet() {
    WorkflowStateMethod method = new WorkflowStateMethod(null,
        new StateParameter("writable", Long.class, null, false, true),
        new StateParameter("writableExplicitlySet", Long.class, null, false, true),
        new StateParameter("readOnly", Long.class, null, true, true));
    Object[] args = new Object[] {
        execution,
        new Mutable<>(42L),
        new Mutable<>(43L),
        new Mutable<>(77L),
    };

    mapper.storeArguments(execution, method, args, Set.of("writableExplicitlySet"));

    verify(execution).setVariable("writable", "42");

    verify(execution, never()).setVariable(eq("writableExplicitlySet"), anyString());
    verify(execution, never()).setVariable(eq("readOnly"), anyString());
  }

  private void verifyParam(Type type, String strVal, Object expectedVal) {
    verifyParam(type, strVal, null, null, expectedVal);
  }

  private void verifyParam(Type type, String strVal, Object defaultVal, Object expectedDefautlVal, Object expectedVal) {
    boolean mutable = expectedDefautlVal instanceof Mutable;
    WorkflowStateMethod method = new WorkflowStateMethod(null, new StateParameter("null", type, defaultVal, false, mutable),
        new StateParameter("key", type, defaultVal, false, mutable));
    when(execution.getVariable("null")).thenReturn(null);
    when(execution.getVariable("key")).thenReturn(strVal);
    Object[] vars = mapper.createArguments(execution, method);
    assertThat(vars[1], is(expectedDefautlVal));
    assertThat(vars[2], is(expectedVal));
  }
}

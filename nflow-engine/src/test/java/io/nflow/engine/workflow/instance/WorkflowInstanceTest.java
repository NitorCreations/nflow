package io.nflow.engine.workflow.instance;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;

import io.nflow.engine.internal.workflow.ObjectStringMapper;

public class WorkflowInstanceTest {

  private final ObjectStringMapper mapper = mock(ObjectStringMapper.class);

  @Test
  public void newWorkflowInstanceNextActivationIsSetByDefault() {
    WorkflowInstance instance = new WorkflowInstance.Builder().build();
    assertThat(instance.nextActivation, is(not(nullValue())));
  }

  @Test(expected = IllegalArgumentException.class)
  public void putStateVariableObjectDoesNotAcceptNullValue() {
    new WorkflowInstance.Builder(mapper).putStateVariable("key", (Object) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void putStateVariableStringDoesNotAcceptNullValue() {
    new WorkflowInstance.Builder().putStateVariable("key", (String) null);
  }

  @Test
  public void putStateVariableOptionalDoesNotSetEmptyValue() {
    new WorkflowInstance.Builder().putStateVariable("key", Optional.empty());
  }

  @Test
  public void putStateVariableOptionalSetsPresentValue() {
    when(mapper.convertFromObject("key", "value")).thenReturn("value");

    WorkflowInstance instance = new WorkflowInstance.Builder(mapper).putStateVariable("key", Optional.of("value")).build();

    assertThat(instance.stateVariables.get("key"), is(equalTo("value")));
  }

  @Test(expected = IllegalStateException.class)
  public void getTypedStateVariableFailsWithoutObjectMapper() {
    WorkflowInstance instance = new WorkflowInstance.Builder().build();

    instance.getStateVariable("key", Long.class);
  }

  @Test
  public void getStateVariableWorks() {
    WorkflowInstance instance = new WorkflowInstance.Builder().putStateVariable("key", "value").build();

    assertThat(instance.getStateVariable("key"), is("value"));
  }

  @Test
  public void getStateVariableWithDefaultValueWorks() {
    WorkflowInstance instance = new WorkflowInstance.Builder().build();

    assertThat(instance.getStateVariable("key", "defaultValue"), is("defaultValue"));
  }

  @Test
  public void getTypedStateVariableWorks() {
    WorkflowInstance instance = new WorkflowInstance.Builder(mapper).putStateVariable("key", 42L).build();
    when(mapper.convertToObject(Long.class, "key", null)).thenReturn(42L);

    assertThat(instance.getStateVariable("key", Long.class), is(42L));
  }

  @Test
  public void getTypedStateVariableWithDefaultValueWorks() {
    WorkflowInstance instance = new WorkflowInstance.Builder(mapper).build();

    assertThat(instance.getStateVariable("key", Long.class, 99L), is(99L));
  }

}

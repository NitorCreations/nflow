package io.nflow.engine.workflow.instance;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.workflow.instance.WorkflowInstance.Builder;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowInstanceFactoryTest {
  @Mock
  ObjectStringMapper objectMapper;
  WorkflowInstanceFactory factory;

  @Before
  public void setup() {
    factory = new WorkflowInstanceFactory(objectMapper);
  }

  @Test
  public void newWorkflowInstanceBuilder() {
    Builder builder = factory.newWorkflowInstanceBuilder();
    assertThat(builder, is(notNullValue()));
    builder.build();
  }

}

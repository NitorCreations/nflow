package com.nitorcreations.nflow.engine.workflow.instance;

import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.Builder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

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

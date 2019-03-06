package io.nflow.engine.workflow.instance;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.workflow.instance.WorkflowInstance.Builder;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WorkflowInstanceFactoryTest {
  @Mock
  ObjectStringMapper objectMapper;
  WorkflowInstanceFactory factory;

  @BeforeEach
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

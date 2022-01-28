package io.nflow.engine.workflow.instance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.workflow.instance.WorkflowInstance.Builder;

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

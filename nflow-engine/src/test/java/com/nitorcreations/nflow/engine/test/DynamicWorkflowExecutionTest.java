package com.nitorcreations.nflow.engine.test;

import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.finished;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nitorcreations.nflow.engine.internal.config.EngineConfiguration;
import com.nitorcreations.nflow.engine.internal.storage.db.H2DatabaseConfiguration;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;
import com.nitorcreations.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinitionBuilder;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("nflow.db.h2")
@ContextConfiguration(classes = { H2DatabaseConfiguration.class, EngineConfiguration.class })
@DirtiesContext
public class DynamicWorkflowExecutionTest {

  static AbstractWorkflowDefinition<WorkflowState> def;

  @Inject
  private WorkflowDefinitionService defs;
  @Inject
  private WorkflowInstanceService instanceService;
  @Inject
  private WorkflowInstanceFactory factory;

  @BeforeClass
  public static void define() {
    def = new WorkflowDefinitionBuilder("dynamic")
        .setInitialState(Initial.class, "method", "start")
        .setErrorState(WorkflowStateType.manual, "error")
        .addState(EndState.class, "end", end, "ending", "End Description").build();
  }

  @Before
  public void setup() throws IOException, ReflectiveOperationException {
    defs.addWorkflowDefinition(def);
    defs.postProcessWorkflowDefinitions();
  }

  @Component
  public static class Initial {
    @Inject
    EndState next;

    public NextAction method(StateExecution execution) {
      return NextAction.moveToState(def.getState("ending"), "reason");
    }
  }

  @Component
  public static class EndState {
    public void end(StateExecution execution) {
    }
  }

  @Test(timeout = 10000)
  public void test() throws InterruptedException {
    WorkflowInstance instance = factory.newWorkflowInstanceBuilder().setType("dynamic").setNextActivation(new DateTime()).build();
    int id = instanceService.insertWorkflowInstance(instance);
    do {
      SECONDS.sleep(1);
      instance = instanceService.getWorkflowInstance(id);
    } while (instance.status != finished);
    assertThat(instance.state, is("ending"));
  }
}

package com.nitorcreations.nflow.rest.v1.converter;

import static com.nitorcreations.Matchers.reflectEquals;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;
import com.nitorcreations.nflow.rest.v1.converter.ListWorkflowInstanceConverter;
import com.nitorcreations.nflow.rest.v1.msg.Action;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;

@RunWith(MockitoJUnitRunner.class)
public class ListWorkflowInstanceConverterTest {

  private ListWorkflowInstanceConverter converter;

  @Before
  public void setup() {
    converter = new ListWorkflowInstanceConverter();
  }

  @Test
  public void convertWithActionsWorks() {
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setState("oState").setStateText("oState desc").
        setRetryNo(1).setExecutionStart(now().minusDays(1)).setExecutionEnd(now().plusDays(1)).build();
    WorkflowInstance i = new WorkflowInstance.Builder().setId(1).setType("dummy").setBusinessKey("businessKey").
        setState("cState").setStateText("cState desc").setNextActivation(now()).setActions(Arrays.asList(a)).build();

    ListWorkflowInstanceResponse resp = converter.convert(i, new QueryWorkflowInstances.Builder().setIncludeActions(true).build());
    assertThat(resp.id, is(i.id));
    assertThat(resp.type, is(i.type));
    assertThat(resp.businessKey, is(i.businessKey));
    assertThat(resp.state, is(i.state));
    assertThat(resp.stateText, is(i.stateText));
    assertThat(resp.nextActivation, is(i.nextActivation));
    assertThat(resp.actions, contains(reflectEquals(new Action(a.state, a.stateText, a.retryNo,
        a.executionStart, a.executionEnd))));
  }

  @Test
  public void convertWithoutActionsWorks() {
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setState("oState").setStateText("oState desc").
        setRetryNo(1).setExecutionStart(now().minusDays(1)).setExecutionEnd(now().plusDays(1)).build();
    WorkflowInstance i = new WorkflowInstance.Builder().setId(1).setType("dummy").setBusinessKey("businessKey").
        setState("cState").setStateText("cState desc").setNextActivation(now()).setActions(Arrays.asList(a)).build();

    ListWorkflowInstanceResponse resp = converter.convert(i, new QueryWorkflowInstances.Builder().build());
    assertThat(resp.id, is(i.id));
    assertThat(resp.type, is(i.type));
    assertThat(resp.businessKey, is(i.businessKey));
    assertThat(resp.state, is(i.state));
    assertThat(resp.stateText, is(i.stateText));
    assertThat(resp.nextActivation, is(i.nextActivation));
    assertThat(resp.actions, nullValue());
  }

}

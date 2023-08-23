package io.nflow.rest.v1.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;

@ExtendWith(MockitoExtension.class)
public class CreateWorkflowConverterTest {

  @Spy
  private final ObjectStringMapper objectMapper = new ObjectStringMapper(ObjectMapper::new);

  private CreateWorkflowConverter converter;

  @BeforeEach
  public void setup() {
    converter = new CreateWorkflowConverter(new WorkflowInstanceFactory(objectMapper));
  }

  @Test
  public void convertAndValidateWorks() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.activationTime = DateTime.now();
    req.businessKey = "businessKey";
    req.externalId = "externalId";
    req.type = "wfType";
    req.startState = "startState";
    req.stateVariables.put("foo", "bar");
    req.stateVariables.put("textNode", new TextNode("text"));
    WorkflowInstance i = converter.convert(req);
    assertThat(i.nextActivation, equalTo(req.activationTime));
    assertThat(i.businessKey, equalTo(req.businessKey));
    assertThat(i.externalId, equalTo(req.externalId));
    assertThat(i.type, equalTo(req.type));
    assertThat(i.state, equalTo("startState"));
    assertThat(i.stateVariables.get("foo"), is("bar"));
    assertThat(i.stateVariables.get("textNode"), is("\"text\""));
  }

  @Test
  public void convertAndValidateWorksWithMinimalData() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = "wfType";
    WorkflowInstance i = converter.convert(req);
    assertThat(i.nextActivation, notNullValue(DateTime.class));
    assertThat(i.businessKey, nullValue(String.class));
    assertThat(i.externalId, nullValue(String.class));
    assertThat(i.type, equalTo(req.type));
  }

  @Test
  public void nextActivationIsSetToNullWhenInstanceIsNotActivated() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = "wfType";
    req.activationTime = now();
    req.activate = false;
    WorkflowInstance i = converter.convert(req);
    assertThat(i.nextActivation, nullValue(DateTime.class));
    assertThat(i.businessKey, nullValue(String.class));
    assertThat(i.externalId, nullValue(String.class));
    assertThat(i.type, equalTo(req.type));
  }

  @Test
  public void convertWorks() {
    WorkflowInstance i = new WorkflowInstance.Builder().setId(1).setType("dummy").setBusinessKey("businessKey")
        .setExternalId("externalId").build();
    CreateWorkflowInstanceResponse resp = converter.convert(i);
    assertThat(resp.id, equalTo(i.id));
    assertThat(resp.type, equalTo(i.type));
    assertThat(resp.businessKey, equalTo(i.businessKey));
    assertThat(resp.externalId, equalTo(i.externalId));
  }

}

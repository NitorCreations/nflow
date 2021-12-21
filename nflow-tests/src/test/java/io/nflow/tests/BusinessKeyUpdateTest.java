package io.nflow.tests;

import static io.nflow.tests.demo.workflow.BusinessKeyWorkflow.BUSINESS_KEY_WORKFLOW_TYPE;
import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.ComponentScan;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.demo.workflow.DemoWorkflow;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BusinessKeyUpdateTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder().springContextClass(DemoConfiguration.class).build();

  private static CreateWorkflowInstanceResponse resp;

  public BusinessKeyUpdateTest() {
    super(server);
  }

  @ComponentScan(basePackageClasses = DemoWorkflow.class)
  static class DemoConfiguration {
    // for component scanning only
  }

  @Test
  @Order(1)
  public void createWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = BUSINESS_KEY_WORKFLOW_TYPE;
    req.businessKey = "originalKey";
    resp = createWorkflowInstance(req);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(2)
  public void checkBusinessKeyWasUpdated() {
    ListWorkflowInstanceResponse wfr = assertTimeoutPreemptively(ofSeconds(5), () -> {
      ListWorkflowInstanceResponse wf = null;
      do {
        sleep(200);
        ListWorkflowInstanceResponse[] instances = getInstanceResource().query("type", BUSINESS_KEY_WORKFLOW_TYPE)
            .get(ListWorkflowInstanceResponse[].class);
        assertThat(instances.length, greaterThanOrEqualTo(1));
        for (ListWorkflowInstanceResponse instance : instances) {
          if (instance.id == resp.id && "done".equals(instance.state) && instance.nextActivation == null) {
            wf = instance;
            break;
          }
        }
      } while (wf == null);
      return wf;
    });
    assertThat(wfr.businessKey, is(equalTo("newBusinessKey")));
  }
}

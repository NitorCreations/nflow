package io.nflow.rest.v1;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.nflow.engine.service.WorkflowInstanceInclude;

public class ApiWorkflowInstanceIncludeTest {

  @Test
  void allWorkflowInstanceIncludeEnumValuesAreMapped() {
    Set<WorkflowInstanceInclude> mapped = stream(ApiWorkflowInstanceInclude.values())
        .map(ApiWorkflowInstanceInclude::getInclude)
        .collect(toSet());
    assertThat(mapped, containsInAnyOrder(WorkflowInstanceInclude.values()));
  }
}

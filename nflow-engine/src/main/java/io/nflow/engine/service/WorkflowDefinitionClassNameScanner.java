package io.nflow.engine.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.core.io.AbstractResource;
import org.springframework.stereotype.Component;

import io.nflow.engine.config.NFlow;
import io.nflow.engine.workflow.definition.WorkflowDefinition;

/**
 * Register workflow definitions defined in the class name listing resource.
 */
@Component
public class WorkflowDefinitionClassNameScanner {

  private static final Logger logger = getLogger(WorkflowDefinitionClassNameScanner.class);

  @Inject
  public WorkflowDefinitionClassNameScanner(WorkflowDefinitionService workflowDefinitionService,
      @Nullable @NFlow AbstractResource classNameListing) throws IOException, ReflectiveOperationException {
    if (classNameListing == null) {
      logger.info("No non-Spring workflow definitions");
    } else {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(classNameListing.getInputStream(), UTF_8))) {
        String row;
        while ((row = br.readLine()) != null) {
          logger.info("Preparing workflow {}", row);
          @SuppressWarnings("unchecked")
          Class<WorkflowDefinition> clazz = (Class<WorkflowDefinition>) Class.forName(row);
          workflowDefinitionService.addWorkflowDefinition(clazz.getDeclaredConstructor().newInstance());
        }
      }
    }
  }
}

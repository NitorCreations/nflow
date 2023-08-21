package io.nflow.rest.v1.jaxrs;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_DEFINITION_PATH;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_DEFINITION_TAG;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.ok;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.converter.ListWorkflowDefinitionConverter;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path(NFLOW_WORKFLOW_DEFINITION_PATH)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Component
@NflowCors
@Tag(name = NFLOW_WORKFLOW_DEFINITION_TAG)
public class WorkflowDefinitionResource extends JaxRsResource {

  private final WorkflowDefinitionService workflowDefinitions;
  private final ListWorkflowDefinitionConverter converter;
  private final WorkflowDefinitionDao workflowDefinitionDao;

  @Inject
  public WorkflowDefinitionResource(WorkflowDefinitionService workflowDefinitions, ListWorkflowDefinitionConverter converter,
      WorkflowDefinitionDao workflowDefinitionDao) {
    this.workflowDefinitions = workflowDefinitions;
    this.converter = converter;
    this.workflowDefinitionDao = workflowDefinitionDao;
  }

  @GET
  @Operation(summary = "List workflow definitions",
      description = "Returns workflow definition(s): all possible states, transitions between states and other setting metadata. "
          + "The workflow definition can deployed in nFlow engine or historical workflow definition stored in the database.")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = ListWorkflowDefinitionResponse.class))))
  public Response listWorkflowDefinitions(
      @QueryParam("type") @Parameter(description = "Included workflow types") List<String> types) {
    return handleExceptions(
        () -> ok(super.listWorkflowDefinitions(types, workflowDefinitions, converter, workflowDefinitionDao)));
  }
}

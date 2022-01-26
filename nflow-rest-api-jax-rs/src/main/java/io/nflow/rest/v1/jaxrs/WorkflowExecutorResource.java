package io.nflow.rest.v1.jaxrs;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_EXECUTOR_PATH;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_EXECUTOR_TAG;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ok;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.WorkflowExecutorService;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.converter.ListWorkflowExecutorConverter;
import io.nflow.rest.v1.msg.ListWorkflowExecutorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path(NFLOW_WORKFLOW_EXECUTOR_PATH)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Component
@NflowCors
public class WorkflowExecutorResource extends JaxRsResource {

  private final WorkflowExecutorService workflowExecutors;
  private final ListWorkflowExecutorConverter converter;

  @Inject
  public WorkflowExecutorResource(WorkflowExecutorService workflowExecutors, ListWorkflowExecutorConverter converter) {
    this.workflowExecutors = workflowExecutors;
    this.converter = converter;
  }

  @GET
  @Operation(summary = "List workflow executors")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = ListWorkflowExecutorResponse.class))))
  @Tag(name = NFLOW_WORKFLOW_EXECUTOR_TAG)
  public Response listWorkflowExecutors() {
    return handleExceptions(
        () -> ok(workflowExecutors.getWorkflowExecutors().stream().map(converter::convert).collect(toList())));
  }
}

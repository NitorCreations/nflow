package com.nitorcreations.nflow.rest.v1;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.rest.v1.converter.ListWorkflowDefinitionConverter;
import com.nitorcreations.nflow.rest.v1.converter.WorkflowDefinitionStatisticsConverter;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path("/v1/workflow-definition")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "/workflow-definition", description = "Query installed workflow definitions")
@Component
public class WorkflowDefinitionResource {

  private final WorkflowDefinitionService workflowDefinitions;
  private final ListWorkflowDefinitionConverter converter;
  private final WorkflowDefinitionStatisticsConverter statisticsConverter;

  @Inject
  public WorkflowDefinitionResource(WorkflowDefinitionService workflowDefinitions, ListWorkflowDefinitionConverter converter, WorkflowDefinitionStatisticsConverter statisticsConverter) {
    this.workflowDefinitions = workflowDefinitions;
    this.converter = converter;
    this.statisticsConverter = statisticsConverter;
  }

  @GET
  @ApiOperation(value = "List workflow definitions", response = ListWorkflowDefinitionResponse.class, responseContainer = "List")
  public Collection<ListWorkflowDefinitionResponse> listWorkflowInstances(@QueryParam("type") String[] types) {
    List<WorkflowDefinition<? extends WorkflowState>> definitions = workflowDefinitions.getWorkflowDefinitions();
    Set<String> reqTypes = new LinkedHashSet<>(Arrays.asList(types));
    Collection<ListWorkflowDefinitionResponse> response = new ArrayList<>();
    for (WorkflowDefinition<? extends WorkflowState> definition : definitions) {
      if (!reqTypes.isEmpty() && !reqTypes.contains(definition.getType())) {
        continue;
      }
      response.add(converter.convert(definition));
    }
    return response;
  }

  @GET
  @Path("/{type}/statistics")
  @ApiOperation(value = "Get workflow definition statistics", response = WorkflowDefinitionStatisticsResponse.class)
  public WorkflowDefinitionStatisticsResponse getStatistics(@PathParam("type") String type) {
    return statisticsConverter.convert(workflowDefinitions.getStatistics(type));
  }
}

package com.nitorcreations.nflow.rest.v1;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowDefinitionDao;
import com.nitorcreations.nflow.engine.internal.workflow.StoredWorkflowDefinition;
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
  private final WorkflowDefinitionDao workflowDefinitionDao;

  @Inject
  public WorkflowDefinitionResource(WorkflowDefinitionService workflowDefinitions, ListWorkflowDefinitionConverter converter,
      WorkflowDefinitionStatisticsConverter statisticsConverter, WorkflowDefinitionDao workflowDefinitionDao) {
    this.workflowDefinitions = workflowDefinitions;
    this.converter = converter;
    this.statisticsConverter = statisticsConverter;
    this.workflowDefinitionDao = workflowDefinitionDao;
  }

  @GET
  @ApiOperation(value = "List workflow definitions", response = ListWorkflowDefinitionResponse.class, responseContainer = "List")
  public List<ListWorkflowDefinitionResponse> listWorkflowDefinitions(@QueryParam("type") String[] types) {
    List<WorkflowDefinition<? extends WorkflowState>> definitions = workflowDefinitions.getWorkflowDefinitions();
    Set<String> reqTypes = new HashSet<>(asList(types));
    Set<String> foundTypes = new HashSet<>();
    List<ListWorkflowDefinitionResponse> response = new ArrayList<>();
    for (WorkflowDefinition<? extends WorkflowState> definition : definitions) {
      if (reqTypes.isEmpty() || reqTypes.contains(definition.getType())) {
        foundTypes.add(definition.getType());
        response.add(converter.convert(definition));
      }
    }
    if (reqTypes.isEmpty() || foundTypes.size() < reqTypes.size()) {
      reqTypes.removeAll(foundTypes);
      List<StoredWorkflowDefinition> storedDefinitions = workflowDefinitionDao.queryStoredWorkflowDefinitions(reqTypes);
      for (StoredWorkflowDefinition storedDefinition : storedDefinitions) {
        if (!foundTypes.contains(storedDefinition.type)) {
          response.add(converter.convert(storedDefinition));
        }
      }
    }
    sort(response);
    return response;
  }

  @GET
  @Path("/{type}/statistics")
  @ApiOperation(value = "Get workflow definition statistics", response = WorkflowDefinitionStatisticsResponse.class)
  public WorkflowDefinitionStatisticsResponse getStatistics(@PathParam("type") String type,
      @QueryParam("createdAfter") DateTime createdAfter, @QueryParam("createdBefore") DateTime createdBefore,
      @QueryParam("modifiedAfter") DateTime modifiedAfter, @QueryParam("modifiedBefore") DateTime modifiedBefore) {
    return statisticsConverter.convert(workflowDefinitions.getStatistics(type, createdAfter, createdBefore, modifiedAfter,
        modifiedBefore));
  }
}

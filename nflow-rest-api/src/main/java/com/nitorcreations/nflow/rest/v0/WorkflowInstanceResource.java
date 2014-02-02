package com.nitorcreations.nflow.rest.v0;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.rest.v0.converter.CreateWorkflowConverter;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceResponse;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path("/workflow-instance")
@Produces("application/json")
@Api(value = "/workflow-instance", description = "Manage workflow instances")
public class WorkflowInstanceResource 
{
  private RepositoryService repositoryService;
  private CreateWorkflowConverter converter;
	
  @Inject
  public void setRepositoryService(
      RepositoryService repositoryService,
      CreateWorkflowConverter converter) {
    this.repositoryService = repositoryService;
    this.converter = converter;
  }
  
	 @PUT
	 @ApiOperation("Submit new workflow instance")
  public CreateWorkflowInstanceResponse createWorkflowInstance(@Valid CreateWorkflowInstanceRequest req) {
	   WorkflowInstance instance = converter.convertAndValidate(req);        
	   int id = repositoryService.insertWorkflowInstance(instance);
	   return converter.convert(id, instance);
	 }
  
}

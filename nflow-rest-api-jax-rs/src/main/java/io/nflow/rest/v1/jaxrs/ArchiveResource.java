package io.nflow.rest.v1.jaxrs;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.ArchiveService;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.msg.ArchiveRequest;
import io.nflow.rest.v1.msg.ArchiveResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/nflow/v1/archive")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api("nFlow archiving")
@Component
@NflowCors
public class ArchiveResource {

  @Inject
  private ArchiveService archiveService;

  @POST
  @ApiOperation("Archive workflow instances synchronously")
  public ArchiveResponse archiveWorkflows(
      @ApiParam(value = "Parameters for the archiving process", required = true) ArchiveRequest request) {
    ArchiveResponse response = new ArchiveResponse();
    response.archivedWorkflows = archiveService.archiveWorkflows(request.olderThan, request.batchSize);
    return response;
  }
}

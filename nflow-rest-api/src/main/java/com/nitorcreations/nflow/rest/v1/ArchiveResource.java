package com.nitorcreations.nflow.rest.v1;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.ArchiveService;
import com.nitorcreations.nflow.rest.v1.msg.ArchiveRequest;
import com.nitorcreations.nflow.rest.v1.msg.ArchiveResponse;

@Path("/v1/archive")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api("Archiving")
@Component
public class ArchiveResource {

  @Inject
  private ArchiveService archiveService;

  @POST
  @ApiOperation("Archive workflows")
  public ArchiveResponse archiveWorkflows(ArchiveRequest request) {
    ArchiveResponse response = new ArchiveResponse();
    response.archivedWorkflows = archiveService.archiveWorkflows(request.olderThan, request.batchSize);
    return response;
  }
}

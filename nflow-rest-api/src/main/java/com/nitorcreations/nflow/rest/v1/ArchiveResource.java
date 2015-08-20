package com.nitorcreations.nflow.rest.v1;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.ArchiveService;
import com.nitorcreations.nflow.rest.v1.msg.ArchiveRequest;

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
  public Response archiveWorkflows(ArchiveRequest request) {
    Integer archivedWorkflows = archiveService.archiveWorkflows(request.olderThan, request.batchSize);
    return Response.ok().header("X-Archived-Workflows", archivedWorkflows).build();
  }
}

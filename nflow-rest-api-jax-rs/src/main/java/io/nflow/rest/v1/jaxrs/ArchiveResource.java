package io.nflow.rest.v1.jaxrs;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_ARCHIVE_PATH;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

import io.nflow.rest.config.jaxrs.NflowCors;
import io.swagger.annotations.Api;

// TODO: replace this with new CleanupResource

@Path(NFLOW_ARCHIVE_PATH)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api("nFlow archiving")
@Component
@NflowCors
public class ArchiveResource {

  // @Inject
  // private ArchiveService archiveService;

  // @POST
  // @ApiOperation("Archive workflow instances synchronously")
  // public ArchiveResponse archiveWorkflows(
  // @ApiParam(value = "Parameters for the archiving process", required = true) ArchiveRequest request) {
  // ArchiveResponse response = new ArchiveResponse();
  // // response.archivedWorkflows = archiveService.archiveWorkflows(request.olderThan, request.batchSize);
  // return response;
  // }
}

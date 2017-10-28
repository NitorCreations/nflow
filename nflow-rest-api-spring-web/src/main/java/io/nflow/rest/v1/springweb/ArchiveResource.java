package io.nflow.rest.v1.springweb;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_ARCHIVE_PATH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.nflow.engine.service.ArchiveService;
import io.nflow.rest.v1.msg.ArchiveRequest;
import io.nflow.rest.v1.msg.ArchiveResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping(value = NFLOW_ARCHIVE_PATH, produces = APPLICATION_JSON_VALUE)
@Api("nFlow archiving")
@Component
public class ArchiveResource {

  @Autowired
  private ArchiveService archiveService;

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  @ApiOperation("Archive workflow instances synchronously")
  public ArchiveResponse archiveWorkflows(
      @RequestBody @ApiParam(value = "Parameters for the archiving process", required = true) ArchiveRequest request) {
    ArchiveResponse response = new ArchiveResponse();
    response.archivedWorkflows = archiveService.archiveWorkflows(request.olderThan, request.batchSize);
    return response;
  }
}

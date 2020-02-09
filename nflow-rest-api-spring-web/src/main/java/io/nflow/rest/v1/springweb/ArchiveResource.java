package io.nflow.rest.v1.springweb;

import static io.nflow.rest.config.springweb.PathConstants.NFLOW_SPRING_WEB_PATH_PREFIX;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_ARCHIVE_PATH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;

// TODO: Replace this with new CleanupResource

@RestController
@RequestMapping(value = NFLOW_SPRING_WEB_PATH_PREFIX + NFLOW_ARCHIVE_PATH, produces = APPLICATION_JSON_VALUE)
@Api("nFlow archiving")
@Component
public class ArchiveResource {
  // @Autowired
  // private ArchiveService archiveService;
  //
  // @PostMapping(consumes = APPLICATION_JSON_VALUE)
  // @ApiOperation("Archive workflow instances synchronously")
  // @SuppressWarnings("deprecation")
  // public ArchiveResponse archiveWorkflows(
  // @RequestBody @ApiParam(value = "Parameters for the archiving process", required = true) ArchiveRequest request) {
  // ArchiveResponse response = new ArchiveResponse();
  // response.archivedWorkflows = archiveService.archiveWorkflows(request.olderThan, request.batchSize);
  // return response;
  // }
}

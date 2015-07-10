package com.nitorcreations.nflow.engine.service;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import com.nitorcreations.nflow.engine.internal.dao.ArchiveDao;
import org.springframework.util.Assert;

@Named
public class ArchiveService {
  @Inject
  private ArchiveDao archiveDao;

  public int archiveWorkflows(DateTime olderThan, int batchSize) {
    Assert.notNull(olderThan, "olderThan must not be null");
    Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");

    List<Integer> workflowIds;
    int archivedWorkflows = 0;
    do {
      workflowIds = archiveDao.listArchivableWorkflows(olderThan, batchSize);
      if(workflowIds.isEmpty()) {
        break;
      }
      archiveDao.archiveWorkflows(workflowIds);
      archivedWorkflows += workflowIds.size();
    } while(!workflowIds.isEmpty());

    return archivedWorkflows;
  }
}

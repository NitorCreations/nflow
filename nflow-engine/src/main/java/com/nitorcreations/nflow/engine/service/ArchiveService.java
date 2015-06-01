package com.nitorcreations.nflow.engine.service;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import com.nitorcreations.nflow.engine.internal.dao.ArchiveDao;

@Named
public class ArchiveService {
  @Inject
  private ArchiveDao archiveDao;

  public int archiveWorkflows(DateTime olderThan, int batchSize) {
    List<Integer> workflowIds;
    int archivedWorkflows = 0;
    do {
      workflowIds = archiveDao.listArchivableWorkflows(olderThan, batchSize);
      archiveDao.archiveWorkflows(workflowIds);
      archivedWorkflows += workflowIds.size();
    } while(!workflowIds.isEmpty());

    return archivedWorkflows;
  }
}

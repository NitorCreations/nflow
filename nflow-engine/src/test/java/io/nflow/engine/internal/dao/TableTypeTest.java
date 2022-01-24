package io.nflow.engine.internal.dao;

import static io.nflow.engine.internal.dao.TableType.convertMainToArchive;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class TableTypeTest {

    @Test
    public void convertMainToArchiveWorks() {
      assertThat(convertMainToArchive("select * from nflow_workflow_action where nflow_workflow_action.id = 1"),
          is("select * from nflow_archive_workflow_action where nflow_archive_workflow_action.id = 1"));
      assertThat(convertMainToArchive("select * from nflow_workflow_state where nflow_workflow_state.id = 1"),
          is("select * from nflow_archive_workflow_state where nflow_archive_workflow_state.id = 1"));
      assertThat(convertMainToArchive("select * from nflow_workflow where nflow_workflow.id = 1"),
          is("select * from nflow_archive_workflow where nflow_archive_workflow.id = 1"));
    }
}

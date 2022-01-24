package io.nflow.engine.internal.dao;

import static io.nflow.engine.internal.dao.TableType.ARCHIVE;
import static io.nflow.engine.internal.dao.TableType.MAIN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class TableTypeTest {

    @Test
    public void replaceAllWorks() {
      assertThat("select * from nflow_workflow_state where".replaceAll(MAIN.prefix, ARCHIVE.prefix),
          is("select * from nflow_archive_workflow_state where"));
      assertThat("select * from nflow_workflow_action where".replaceAll(MAIN.prefix, ARCHIVE.prefix),
          is("select * from nflow_archive_workflow_action where"));
      assertThat("select * from nflow_workflow where".replaceAll(MAIN.prefix, ARCHIVE.prefix),
          is("select * from nflow_archive_workflow where"));
    }
}

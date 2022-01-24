package io.nflow.engine.internal.dao;

import org.junit.jupiter.api.Test;

import static io.nflow.engine.internal.dao.NflowTables.ARCHIVE;
import static io.nflow.engine.internal.dao.NflowTables.MAIN;
import static io.nflow.engine.internal.dao.NflowTables.asArchiveTable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class NflowTablesTest {

    @Test
    public void asArchiveTableWorks() {
        assertThat(asArchiveTable(MAIN.workflow), is(ARCHIVE.workflow));
        assertThat(asArchiveTable(MAIN.workflow_state), is(ARCHIVE.workflow_state));
        assertThat(asArchiveTable(MAIN.workflow_action), is(ARCHIVE.workflow_action));
    }

    @Test
    public void replaceAllWorks() {
        assertThat(MAIN.replaceAll("select * from nflow_workflow_state where", ARCHIVE), is("select * from nflow_archive_workflow_state where"));
        assertThat(MAIN.replaceAll("select * from nflow_workflow_action where", ARCHIVE), is("select * from nflow_archive_workflow_action where"));
        assertThat(MAIN.replaceAll("select * from nflow_workflow where", ARCHIVE), is("select * from nflow_archive_workflow where"));
    }
}
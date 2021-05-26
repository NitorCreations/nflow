import React, { useState, useEffect, useContext, useCallback } from "react";

import { formatAgo, formatTimestamp } from "../utils";
import { ConfigContext } from "../config";
import { DataTable, Spinner } from "../component";
import { Executor } from "../types";
import { listExecutors } from "../service";

function ExecutorListPage() {
  const [initialLoad, setInitialLoad] = useState<boolean>(true);
  const [executors, setExecutors] = useState<Array<Executor>>([]);
  const config = useContext(ConfigContext);

  const fetchExecutors = useCallback(() => {
    listExecutors(config)
      .then((data) => setExecutors(data))
      .catch((error) => {
        // TODO error handling
        console.error("Error", error);
      })
      .finally(() => setInitialLoad(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // refresh list periodically
  useEffect(() => {
    fetchExecutors();
    let handle = setInterval(fetchExecutors, config.refreshSeconds * 1000);
    return () => clearInterval(handle);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const executorTable = () => {
    const columns = [
      { field: 'id', headerName: 'ID'},
      { field: 'host', headerName: 'Host'},
      { field: 'pid', headerName: 'Process ID'},
      { field: 'executorGroup', headerName: 'Executor Group'},
      { field: 'started', headerName: 'Started', fieldRender: formatAgo, tooltipRender: formatTimestamp},
      { field: 'stopped', headerName: 'Stopped', fieldRender: formatAgo, tooltipRender: formatTimestamp},
      { field: 'active', headerName: 'Activity hearbeat', fieldRender: formatAgo, tooltipRender: formatTimestamp},
      { field: 'expires', headerName: 'Hearbeat expires', fieldRender: formatAgo, tooltipRender: formatTimestamp}, ]

    const rows = executors
    return (
      <DataTable rows={rows} columns={columns} />
    )
  };

  return (
    <div>
      <h1>Workflow executors</h1>
      {initialLoad ? <Spinner /> : executorTable()}
    </div>
  );
}

export default ExecutorListPage;

import React, { useState, useEffect, useContext, useCallback } from "react";
import { formatAgo } from "../utils";
import { ConfigContext } from "../config";
import { Spinner } from "../component";
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

  const executorRow = (executor: any) => (
    // TODO colors based on stopped/hearbeat expiry
    <tr key={executor.id}>
      <td>{executor.id}</td>
      <td>{executor.host}</td>
      <td>{executor.pid}</td>
      <td>{executor.executorGroup}</td>
      <td title={executor.started}>{formatAgo(executor.started)}</td>
      <td title={executor.stopped}>{formatAgo(executor.stopped)}</td>
      <td title={executor.active}>{formatAgo(executor.active)}</td>
      <td title={executor.expires}>{formatAgo(executor.expires)}</td>
    </tr>
  );

  const executorTable = () => (
    <table>
      <thead>
        <tr>
          <th>Id</th>
          <th>Host</th>
          <th>Process ID</th>
          <th>Executor group</th>
          <th>Started</th>
          <th>Stopped</th>
          <th>Activity hearbeat</th>
          <th>Hearbeat expires</th>
        </tr>
      </thead>
      <tbody>{executors.map(executorRow)}</tbody>
    </table>
  );

  // TODO make a spinner component
  return (
    <div>
      <h1>Workflow executors</h1>
      {initialLoad ? <Spinner /> : executorTable()}
    </div>
  );
}

export default ExecutorListPage;

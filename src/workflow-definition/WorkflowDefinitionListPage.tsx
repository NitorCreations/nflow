import React, { useState, useEffect, useContext, useCallback } from "react";
import { Link } from "react-router-dom";

import { ConfigContext } from "../config";
import { Spinner } from "../component";

import { WorkflowDefinition } from "../types";
import { listWorkflowDefinitions } from "../service";

function WorkflowDefinitionListPage() {
  const config = useContext(ConfigContext);

  const [initialLoad, setInitialLoad] = useState<boolean>(true);
  const [definitions, setDefinitions] = useState<Array<WorkflowDefinition>>([]);

  const fetchDefinitions = useCallback(() => {
    listWorkflowDefinitions(config)
      .then((data) => setDefinitions(data))
      .catch((error) => {
        // TODO error handling
        console.error("Error", error);
      })
      .finally(() => setInitialLoad(false));
  }, []);

  useEffect(() => fetchDefinitions(), []);

  const definitionRow = (definition: any) => {
    const path = "/workflow-definition/" + definition.type;
    return (
      <tr key={definition.type}>
        <td>
          <Link to={path}>{definition.type}</Link>
        </td>
        <td>
          <Link to={path}>
            {definition.description ? (
              definition.description
            ) : (
              <em>No description</em>
            )}
          </Link>
        </td>
      </tr>
    );
  };

  const definitionTable = () => (
    <table>
      <thead>
        <tr>
          <th>Type</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>{definitions.map(definitionRow)}</tbody>
    </table>
  );
  return (
    <div>
      <h1>Workflow definitions</h1>
      {initialLoad ? <Spinner /> : definitionTable()}
    </div>
  );
}

export default WorkflowDefinitionListPage;

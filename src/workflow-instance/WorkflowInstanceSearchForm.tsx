import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";

const statuses = [
  "",
  "created",
  "inProgress",
  "finished",
  "manual",
  "executing",
];

const typeNames: any = {
  "": "-- All workflow types --",
};

const stateNames: any = {
  "": "-- All workflow states --",
};

const statusNames: any = {
  "": "-- All statuses --",
};

function WorkflowInstanceSearchForm(props: {
  definitions: Array<any>;
  onSubmit: (data: any) => any;
}) {
  const queryParams = new URLSearchParams(useLocation().search);
  
  const [type, setType] = useState<string>(queryParams.get("type") || "");
  const [state, setState] = useState<string>(queryParams.get("state") || "");
  const [status, setStatus] = useState<string>(queryParams.get("status") || "");
  const [businessKey, setBusinessKey] = useState<string>(
    queryParams.get("businessKey") || ""
  );
  const [externalId, setExternalId] = useState<string>(
    queryParams.get("externalId") || ""
  );
  const [id, setId] = useState<string>(queryParams.get("id") || "");
  const [parentInstanceId, setParentInstanceId] = useState<string>(
    queryParams.get("parentWorkflowId") || ""
  );

  const types = [""].concat(props.definitions.map((d) => d.type));

  // TODO to lodash or not to lodash?
  const selectedWorkflow = props.definitions.filter((d) => d.type === type)[0];
  const selectedStates = (
    (selectedWorkflow && selectedWorkflow.states) ||
    []
  ).map((s: any) => s.id);
  const states = [""].concat(selectedStates);

  const handleSubmit = (e?: any) => {
    e && e.preventDefault();
    let data: any = {
      type,
      state,
      status,
      businessKey,
      externalId,
      id,
      parentInstanceId,
    };
    for (let field of Object.keys(data)) {
      if (data[field]) {
        data[field] = data[field].trim();
      }
    }
    // remove empty fields, convert ids to numbers
    for (let field of ["instanceId", "parentInstanceId"]) {
      if (field in data) {
        data[field] = parseInt(data[field], 10) || undefined;
      }
    }
    for (let k of Object.keys(data)) {
      if (data[k] === "" || Number.isNaN(data[k]) || data[k] === undefined) {
        delete data[k];
      }
    }
    props.onSubmit(data);
  };

  // if query parameters are give, trigger immediate search
  useEffect(() => {
    if (queryParams.keys().next()?.value) {
      handleSubmit();
    }
  }, []);

  const setWorkflowType = (type: string) => {
    // when type is changed, need to reset all selections that depend on type
    setType(type);
    setState("");
  };

  const selection = (
    items: Array<any>,
    selected: string,
    onChange: (v: string) => any,
    nameMap: any
  ) => {
    return (
      <select value={selected} onChange={(e) => onChange(e.target.value)}>
        {items.map((item) => (
          <option key={item} value={item}>
            {nameMap[item] || item}
          </option>
        ))}
      </select>
    );
  };

  return (
    <form>
      <div>
        <label>
          Workflow type
          {selection(types, type, setWorkflowType, typeNames)}
        </label>
      </div>

      <div>
        <label>
          Workflow state
          {selection(states, state, setState, stateNames)}
        </label>
      </div>

      <div>
        <label>
          Workflow status
          {selection(statuses, status, setStatus, statusNames)}
        </label>
      </div>

      <div>
        <label>
          Business key
          <input
            value={businessKey}
            onChange={(e) => setBusinessKey(e.target.value)}
          />
        </label>
      </div>

      <div>
        <label>
          External id
          <input
            value={externalId}
            onChange={(e) => setExternalId(e.target.value)}
          />
        </label>
      </div>

      <div>
        <label>
          Workflow instance id
          <input
            type="number"
            value={id}
            onChange={(e) => setId(e.target.value)}
          />
        </label>
      </div>

      <div>
        <label>
          Workflow parent instance id
          <input
            type="number"
            value={parentInstanceId}
            onChange={(e) => setParentInstanceId(e.target.value)}
          />
        </label>
      </div>
      <button onClick={handleSubmit}>Search</button>
    </form>
  );
}

export default WorkflowInstanceSearchForm;

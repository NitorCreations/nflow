import React from 'react';
import {ObjectTable} from '../component';
import {Alert} from '@material-ui/lab';
import {WorkflowInstance} from '../types';

function StateVariableTable(props: {instance: WorkflowInstance}) {
  const renderValue = (value: any) => {
    return (
      <pre>
        <code>{JSON.stringify(value, null, 2)}</code>
      </pre>
    );
  };
  const columns = Object.keys(props.instance.stateVariables || {}).map(key => ({
    field: key,
    headerName: key,
    fieldRender: renderValue
  }));

  if (!props.instance.stateVariables) {
    return <Alert severity="info">No state variables</Alert>;
  }

  return (
    <ObjectTable object={props.instance.stateVariables} columns={columns} />
  );
}

export {StateVariableTable};

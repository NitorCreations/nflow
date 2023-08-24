import React, {Fragment} from 'react';
import {formatDistance} from 'date-fns';

import {
  Paper,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  TableContainer
} from '@material-ui/core';
import {WorkflowInstance, WorkflowInstanceAction} from '../types';
import {formatTimestamp} from '../utils';
import {InternalLink} from '../component';

// TODO clicking state highlight state in state graph
const ActionHistoryTable = (props: {
  instance: WorkflowInstance;
  childInstances: WorkflowInstance[];
}) => {
  const clickAction = (action: WorkflowInstanceAction) => {
    // TODO call state graph highlight from here
    console.log('Action clicked', action);
  };

  // Group child instances to to action that created them
  const actionChildInstanceMap: {[key: number]: WorkflowInstance[]} = {};
  props.childInstances.forEach(childInstance => {
    const parentActionId = childInstance.parentActionId as number;
    if (!actionChildInstanceMap[parentActionId]) {
      actionChildInstanceMap[parentActionId] = [];
    }
    actionChildInstanceMap[parentActionId].push(childInstance);
  });

  const actionRow = (
    action: WorkflowInstanceAction,
    index: number,
    actions: WorkflowInstanceAction[]
  ) => {
    // TODO colors
    // stateExecution = green
    // stateExecutionFailed = xxx
    // externalChange = blue
    // recovery = xxx
    const actionNumber = actions.length - index;

    // TODO formatDistance is too coarse, gives "less than minute"
    // TODO service.ts should convert these to Date
    const duration = formatDistance(
      new Date(action.executionStartTime),
      new Date(action.executionEndTime)
    );
    const durationTooltip =
      new Date(action.executionEndTime).getTime() -
      new Date(action.executionStartTime).getTime() +
      ' ms';

    const childWorkflows = actionChildInstanceMap[action.id] || [];
    const description = (
      <Fragment>
        <div>{action.stateText}</div>
        {childWorkflows.length > 0 && (
          <ul>
            {childWorkflows.map(child => (
              <li key={child.id}>
                <InternalLink to={'/workflow/' + child.id}>
                  {child.type} ({child.id})
                </InternalLink>
              </li>
            ))}
          </ul>
        )}
        <div>
          <small>{action.type}</small>
        </div>
      </Fragment>
    );

    const className = () => {
      switch (action.type) {
        case 'stateExecution':
          return 'success';
        case 'stateExecutionFailed':
          return 'danger';
      }
      return 'info';
    };
    return (
      <TableRow key={action.id} className={className()}>
        <TableCell>{actionNumber}</TableCell>
        <TableCell onClick={e => clickAction(action)} className="clickable">
          {action.state}
        </TableCell>
        <TableCell>{description}</TableCell>
        <TableCell>{action.retryNo}</TableCell>
        <TableCell>{formatTimestamp(action.executionStartTime)}</TableCell>
        <TableCell>{formatTimestamp(action.executionEndTime)}</TableCell>
        <TableCell title={durationTooltip}>{duration}</TableCell>
      </TableRow>
    );
  };
  return (
    <Fragment>
      <TableContainer component={Paper}>
        <Table
          aria-label="simple table"
          size="small"
          className="table table-striped table-hover"
        >
          <TableHead>
            <TableRow>
              <TableCell>No</TableCell>
              <TableCell>State</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Retries</TableCell>
              <TableCell>Started</TableCell>
              <TableCell>Finished</TableCell>
              <TableCell>Duration</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {props.instance.actions.map((action, index) =>
              actionRow(action, index, props.instance.actions)
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Fragment>
  );
};

export {ActionHistoryTable};

import React, { Fragment } from "react";
import { formatDuration, formatDistance, Duration } from "date-fns"

import { Paper, Table, TableHead, TableFooter, TableBody, TableRow, TableCell, TableContainer } from '@material-ui/core';
import { WorkflowInstance, WorkflowInstanceAction } from "../types";
import { formatTimestamp } from "../utils";
import { InternalLink } from "../component";

// TODO clicking state highlight state in state graph
const ActionHistoryTable = (props: {instance: WorkflowInstance, childInstances: WorkflowInstance[]}) => {
    console.log('instance', props.instance);
    const clickAction = (action: WorkflowInstanceAction) => {
        // TODO call state graph highlight from here
        console.log('Action clicked', action);
    }

    // Group child instances to to action that created them
    const actionChildInstanceMap:{[key: number]: WorkflowInstance[]} = {};
    props.childInstances.forEach(childInstance => {
        const parentActionId = childInstance.parentActionId as number;
        if (!actionChildInstanceMap[parentActionId]) {
            actionChildInstanceMap[parentActionId] = [];
        }
        actionChildInstanceMap[parentActionId].push(childInstance);
    });

    const actionRow = (action: WorkflowInstanceAction, index: number, actions: WorkflowInstanceAction[]) => {
        // TODO colors
        // stateExecution = green
        // stateExecutionFailed = xxx
        // externalChange = blue
        // recovery = xxx
        const actionNumber = actions.length - index;

        // TODO formatDistance is too coarse, gives "less than minute"
        // TODO service.ts should convert these to Date
        const duration = formatDistance(new Date(action.executionStartTime), new Date(action.executionEndTime));
        const durationTooltip = new Date(action.executionEndTime).getTime() - new Date(action.executionStartTime).getTime() + ' ms';

        const childWorkflows = actionChildInstanceMap[action.id] || [];
        const description = (
            <Fragment>{action.stateText}
                {childWorkflows.length > 0 && (
                    <ul>
                        {childWorkflows.map(child => (
                            <li key={child.id}>
                                <InternalLink to={"/workflow/" + child.id}>{child.type} ({child.id})</InternalLink>
                            </li>
                            ))}
                    </ul>
                )}
            </Fragment>
        );

        return (
            <TableRow key={action.id}>
                <TableCell>{actionNumber}</TableCell>
                <TableCell onClick={(e) => clickAction(action)} className="clickable">{action.state}</TableCell>
                <TableCell>{description}</TableCell>
                <TableCell>{action.retryNo}</TableCell>
                <TableCell>{formatTimestamp(action.executionStartTime)}</TableCell>
                <TableCell>{formatTimestamp(action.executionEndTime)}</TableCell>
                <TableCell title={durationTooltip}>{duration}</TableCell>
            </TableRow>
        )
    }
    return (
        <Fragment>
            <h3>Action history</h3>
            <TableContainer component={Paper}>
                <Table>
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
                        {props.instance.actions.map((action, index) => actionRow(action, index, props.instance.actions))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Fragment>
    )

}

export { ActionHistoryTable };

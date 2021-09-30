import {Typography} from '@material-ui/core';
import React from 'react';
import {WorkflowDefinition, WorkflowInstance} from '../../types';
import {UpdateWorkflowInstanceStateForm} from './UpdateWorkflowInstanceStateForm';

const ManageWorkflowInstancePage = function (props: {
  instance: WorkflowInstance;
  definition: WorkflowDefinition;
}) {
  return (
    <div>
      <Typography variant="h3">Update state</Typography>
      <UpdateWorkflowInstanceStateForm
        instance={props.instance}
        definition={props.definition}
      />
    </div>
  );
};

export {ManageWorkflowInstancePage};

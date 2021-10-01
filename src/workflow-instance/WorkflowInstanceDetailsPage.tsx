import React, {Fragment, useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import {
  AppBar,
  Tab,
  Tabs,
  Typography,
  Grid,
  Container
} from '@material-ui/core';

import {StateGraph, InternalLink, ObjectTable, Spinner} from '../component';
import {WorkflowDefinition, WorkflowInstance} from '../types';
import {useConfig} from '../config';
import {
  getWorkflowDefinition,
  getWorkflowInstance,
  listChildWorkflowInstances
} from '../service';
import {formatTimestamp, formatRelativeTime} from '../utils';
import {StateVariableTable} from './StateVariableTable';
import {ActionHistoryTable} from './ActionHistoryTable';
import {TabPanel} from '../component/TabPanel';
import {ManageWorkflowInstancePage} from './manage/ManageWorkflowInstancePage';
import {isConstructorDeclaration} from 'typescript';

const InstanceSummaryTable = ({
  instance,
  parentInstance,
  externalContent
}: {
  instance: WorkflowInstance;
  parentInstance?: WorkflowInstance;
  externalContent?: any;
}) => {
  // TODO clicking currentState should highlight in state graph
  const parentLink = (x: any) =>
    parentInstance && (
      <InternalLink to={'/workflow/' + parentInstance.id}>
        {parentInstance.type} ({parentInstance.id})
      </InternalLink>
    );

  const columns = [
    {
      field: 'parentWorkflowId',
      headerName: 'Parent workflow',
      fieldRender: parentLink
    },
    {field: 'state', headerName: 'Current state'},
    {field: 'status', headerName: 'Current status'},
    {
      field: 'nextActivation',
      headerName: 'Next activation',
      fieldRender: formatRelativeTime,
      tooltipRender: formatTimestamp
    },
    {field: 'businessKey', headerName: 'Business key'},
    {field: 'externalId', headerName: 'External id'},
    {
      field: 'created',
      headerName: 'Created',
      fieldRender: formatTimestamp,
      tooltipRender: formatRelativeTime
    },
    {
      field: 'started',
      headerName: 'Started',
      fieldRender: formatTimestamp,
      tooltipRender: formatRelativeTime
    },
    {
      field: 'modified',
      headerName: 'Modified',
      fieldRender: formatTimestamp,
      tooltipRender: formatRelativeTime
    }
  ];

  const valueClassRender = (column: any, instance: any) => {
    if (column.field !== 'status') {
      return '';
    }
    switch (instance.status) {
      case 'manual':
        return 'danger';
      case 'finished':
        return 'success';
      case 'inProgress':
        return 'info';
    }
  };
  return (
    <div>
      <ObjectTable
        object={instance}
        columns={columns}
        valueClassRender={valueClassRender}
      />
      <div dangerouslySetInnerHTML={{__html: externalContent}} />
    </div>
  );
};

// TODO if the workflow is active, re-fetch peridodically
// - status executing
// - status create/inProgress and has nextActivation (which is less than 24h away?)
const InstanceSummary = ({
  definition,
  instance,
  parentInstance,
  childInstances,
  externalContent
}: {
  definition: WorkflowDefinition;
  instance: WorkflowInstance;
  parentInstance?: WorkflowInstance;
  childInstances: WorkflowInstance[];
  externalContent: any;
}) => {
  const [selectedTab, setSelectedTab] = React.useState(0);
  const handleChange = (
    event: React.ChangeEvent<{}>,
    newSelectedTab: number
  ) => {
    setSelectedTab(newSelectedTab);
  };
  return (
    <Fragment>
      <Grid item xs={12} sm={6}>
        <Container>
          <Typography variant="h2">
            <InternalLink to={'/workflow-definition/' + instance.type}>
              {instance.type}
            </InternalLink>{' '}
            ({instance.id})
          </Typography>
          <InstanceSummaryTable
            instance={instance}
            parentInstance={parentInstance}
            externalContent={externalContent}
          />
        </Container>
      </Grid>
      <Grid item xs={12} sm={6}>
        <AppBar position="static">
          <Tabs value={selectedTab} onChange={handleChange}>
            <Tab label="Action history" />
            <Tab label="State variables" />
            <Tab label="Manage" />
          </Tabs>
        </AppBar>
        <TabPanel value={selectedTab} index={0}>
          <Container>
            <ActionHistoryTable
              instance={instance}
              childInstances={childInstances}
            />
          </Container>
        </TabPanel>
        <TabPanel value={selectedTab} index={1}>
          <Container>
            <StateVariableTable instance={instance} />
          </Container>
        </TabPanel>
        <TabPanel value={selectedTab} index={2}>
          <ManageWorkflowInstancePage
            instance={instance}
            definition={definition}
          />
        </TabPanel>
      </Grid>
      <Grid item xs={12} sm={6}>
        <Container>
          <StateGraph definition={definition} />
        </Container>
      </Grid>
    </Fragment>
  );
};

function WorkflowInstanceDetailsPage() {
  const config = useConfig();

  const [definition, setDefinition] = useState<WorkflowDefinition>();
  const [instance, setInstance] = useState<WorkflowInstance>();
  const [childInstances, setChildInstances] = useState<WorkflowInstance[]>([]);
  const [parentInstance, setParentInstance] = useState<WorkflowInstance>();
  const [externalContent, setExternalContent] = useState<any>();
  const {id} = useParams() as any;

  // TODO This fetches childWorkflows for ActionHistoryTable
  //      There may be a lot of child workflows => possible performance problem
  useEffect(() => {
    Promise.all([
      getWorkflowInstance(config, id),
      listChildWorkflowInstances(config, id)
    ]).then(([instance, childInstances]) => {
      if (instance.parentWorkflowId) {
        return Promise.all([
          getWorkflowInstance(config, instance.parentWorkflowId),
          getWorkflowDefinition(config, instance.type)
        ]).then(([parent, definition]) => {
          setDefinition(definition);
          setParentInstance(parent);
          setInstance(instance);
          setChildInstances(childInstances);
        });
      }
      return getWorkflowDefinition(config, instance.type)
        .then(definition => {
          setDefinition(definition);
          setParentInstance(undefined);
          setInstance(instance);
          setChildInstances(childInstances);
          return definition;
        })
        .then(definition =>
          Promise.resolve(
            config.customInstanceContent(
              definition,
              instance,
              undefined,
              childInstances
            )
          ).then(content => {
            setExternalContent(content);
          })
        );
    });
  }, [config, id]);

  return (
    <Grid container spacing={3}>
      {definition && instance ? (
        <InstanceSummary
          definition={definition}
          instance={instance}
          childInstances={childInstances}
          parentInstance={parentInstance}
          externalContent={externalContent}
        />
      ) : (
        <Grid item xs={12}>
          <Container>
            <Spinner />
          </Container>
        </Grid>
      )}
    </Grid>
  );
}

export default WorkflowInstanceDetailsPage;

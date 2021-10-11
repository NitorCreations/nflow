import React, {useCallback, useEffect, useState} from 'react';
import {
  Typography,
  Grid,
  Container,
  createTheme,
  MuiThemeProvider
} from '@material-ui/core';
import MUIDataTable from 'mui-datatables';

import WorkflowInstanceSearchForm from './WorkflowInstanceSearchForm';
import {useConfig} from '../config';
import {InternalLink, Spinner} from '../component';
import {formatRelativeTime, formatTimestamp} from '../utils';
import {listWorkflowDefinitions, listWorkflowInstances} from '../service';
import {WorkflowInstance} from '../types';
import './workflow-instance.scss';
import '../index.scss';

const InstanceTable = ({instances}: {instances: WorkflowInstance[]}) => {
  const getMuiTheme = () =>
    createTheme({
      overrides: {
        MUIDataTableBodyRow: {
          root: {
            '&:nth-child(odd)': {
              backgroundColor: '#e7e7e7'
            }
          }
        },
        MUIDataTableBodyCell: {
          root: {
            padding: 8,
            wordBreak: 'break-all'
          }
        }
      }
    });

  // TODO colors
  const idLinkRender = (id: string) => {
    const path = '/workflow/' + id;
    return <InternalLink to={path}>{id}</InternalLink>;
  };

  const typeLinkRender = (type: string) => {
    const path = '/workflow-definition/' + type;
    return <InternalLink to={path}>{type}</InternalLink>;
  };

  const renderTimestamp = (value: string) => {
    return (
      <div title={formatRelativeTime(value)}>{formatTimestamp(value)}</div>
    );
  };

  const columns = [
    {
      name: 'id',
      label: 'Id',
      options: {
        customBodyRender: idLinkRender
      }
    },
    {
      name: 'parentWorkflowId',
      label: 'Parent Id',
      options: {
        customBodyRender: idLinkRender,
        display: false
      }
    },
    {
      name: 'type',
      label: 'Workflow type',
      options: {
        customBodyRender: typeLinkRender
      }
    },
    {name: 'state', label: 'State'},
    {
      name: 'stateText',
      label: 'State text',
      options: {
        display: false
      }
    },
    {name: 'status', label: 'Status'},
    {name: 'businessKey', label: 'Business key'},
    {
      name: 'externalId',
      label: 'External Id',
      options: {
        display: false
      }
    },
    {name: 'retries', label: 'Retries'},
    {
      name: 'started',
      label: 'Started',
      options: {
        customBodyRender: renderTimestamp,
        display: false
      }
    },
    {
      name: 'created',
      label: 'Created',
      options: {
        customBodyRender: renderTimestamp
      }
    },
    {
      name: 'modified',
      label: 'Modified',
      options: {
        customBodyRender: renderTimestamp,
        display: false
      }
    },
    {
      name: 'nextActivation',
      label: 'Next activation',
      options: {
        customBodyRender: renderTimestamp
      }
    },
    {
      name: 'priority',
      label: 'Priority',
      options: {
        display: false
      }
    }
  ];

  const rowClassRender = (status: any): string => {
    switch (status) {
      case 'manual':
        return 'danger';
      case 'finished':
        return 'success';
      case 'inProgress':
        return 'info';
    }
    return '';
  };

  return (
    <MuiThemeProvider theme={getMuiTheme()}>
      <MUIDataTable
        title="Search result"
        data={instances}
        columns={columns}
        options={{
          selectableRows: 'none',
          setRowProps: (row, dataIndex, rowIndex) => {
            const rowClassName = rowClassRender(row[5]); // status-column = 5
            return {
              className: `${rowClassName}`
            };
          },
          setTableProps: () => {
            return {
              className: 'table table-hover'
            };
          }
        }}
      />
    </MuiThemeProvider>
  );
};

function WorkflowInstanceListPage() {
  const config = useConfig();

  const [initialLoad, setInitialLoad] = useState<boolean>(true);
  const [definitions, setDefinitions] = useState<Array<any>>([]);
  const [instances, setInstances] = useState<Array<WorkflowInstance>>([]);

  const fetchDefinitions = useCallback(() => {
    listWorkflowDefinitions(config)
      .then(data => setDefinitions(data))
      .catch(error => {
        // TODO error handling
        console.error('Error', error);
      })
      .finally(() => setInitialLoad(false));
  }, [config]);

  useEffect(() => fetchDefinitions(), [fetchDefinitions]);

  const searchInstances = useCallback(
    (data: any) => {
      console.log('search instances', data);
      listWorkflowInstances(config, data)
        .then(data => setInstances(data))
        .catch(error => {
          // TODO error handling
          console.error('Error', error);
        });
    },
    [config]
  );

  const search = (data: any) => {
    searchInstances(data);
  };

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Container className="search-container">
          <Typography variant="h2" gutterBottom>
            Search workflow instances
          </Typography>
          {initialLoad ? (
            <Spinner />
          ) : (
            <WorkflowInstanceSearchForm
              definitions={definitions}
              onSubmit={search}
            />
          )}
        </Container>
        <InstanceTable instances={instances} />
      </Grid>
    </Grid>
  );
}

export default WorkflowInstanceListPage;

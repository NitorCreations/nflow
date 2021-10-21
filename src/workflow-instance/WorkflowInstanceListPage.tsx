import React, {useCallback, useEffect, useState} from 'react';
import {
  Typography,
  Grid,
  Container,
  createTheme,
  MuiThemeProvider,
  TableRow,
  TableCell
} from '@material-ui/core';
import MUIDataTable, {ExpandButton} from 'mui-datatables';

import WorkflowInstanceSearchForm from './WorkflowInstanceSearchForm';
import {useConfig} from '../config';
import {DataTable, InternalLink, Spinner} from '../component';
import {formatRelativeTime, formatTimestamp} from '../utils';
import {listWorkflowDefinitions, listWorkflowInstances} from '../service';
import {WorkflowInstance} from '../types';
import './workflow-instance.scss';
import '../index.scss';

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
  return <div title={formatRelativeTime(value)}>{formatTimestamp(value)}</div>;
};

const ChildWorkflowTable = ({workflowId}: {workflowId: number}) => {
  const config = useConfig();
  const [childWorkflows, setChildWorkflows] = useState(
    new Array<WorkflowInstance>()
  );
  useEffect(() => {
    const query = {
      parentWorkflowId: workflowId
    };
    listWorkflowInstances(config, query)
      .then(data => setChildWorkflows(data))
      .catch(error => {
        // TODO error handling
        console.error('Error', error);
      });
  }, [config, workflowId]);
  const columns = [
    {
      field: 'id',
      headerName: 'Id',
      fieldRender: idLinkRender
    },
    {
      field: 'type',
      headerName: 'Type',
      fieldRender: typeLinkRender
    },
    {
      field: 'state',
      headerName: 'State'
    },
    {
      field: 'status',
      headerName: 'Status'
    },
    {
      field: 'businessKey',
      headerName: 'Business key'
    },
    {
      field: 'retries',
      headerName: 'Retries'
    },
    {
      field: 'modified',
      headerName: 'Last modified',
      fieldRender: renderTimestamp
    },
    {
      field: 'nextActivation',
      headerName: 'Next activation',
      fieldRender: renderTimestamp
    }
  ];
  return <DataTable columns={columns} rows={childWorkflows} />;
};

const InstanceTable = ({instances}: {instances: WorkflowInstance[]}) => {
  const config = useConfig();

  const getMuiTheme = () =>
    createTheme({
      overrides: {
        MUIDataTableBodyCell: {
          root: {
            padding: 8,
            wordBreak: 'break-all'
          }
        }
      }
    });

  const getUserDefinedColumns = (columns: any[]) => {
    if (config.searchResultColumns) {
      const newColumns: any[] = [];
      const staticColumns = ['id', 'type'];

      // preserve static columns and hide all others
      columns.forEach((column, i) => {
        if (staticColumns.includes(column.name)) {
          newColumns.push(column);
          delete columns[i];
        } else {
          column.options = Object.assign({}, column.options, {display: false});
        }
      });

      // pick user defined columns and create new columns for state variables
      config.searchResultColumns.forEach(userColumnDefinition => {
        let tmp = columns.find(
          col => col && col.name === userColumnDefinition.field
        );
        if (!tmp && userColumnDefinition.field.startsWith('stateVariables')) {
          tmp = {
            name: userColumnDefinition.field,
            label: userColumnDefinition.label,
            options: {
              filter: false
            }
          };
        } else {
          delete columns[
            columns.findIndex(
              col => col && col.name === userColumnDefinition.field
            )
          ];
        }
        if (tmp) {
          tmp.options.display = true;
          newColumns.push(tmp);
        } else {
          console.error(
            `Unsupported column name ${userColumnDefinition.field}`
          );
        }
      });

      // final columns are static columns, user defined columns and the rest of the internal columns
      return newColumns.concat(columns.filter(v => v));
    }
    return columns;
  };

  let columns: any[] = getUserDefinedColumns([
    {
      name: 'id',
      label: 'Id',
      options: {
        customBodyRender: idLinkRender,
        filter: false
      }
    },
    {
      name: 'parentWorkflowId',
      label: 'Parent Id',
      options: {
        customBodyRender: idLinkRender,
        display: false,
        filter: false
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
    {
      name: 'businessKey',
      label: 'Business key',
      options: {
        filter: false
      }
    },
    {
      name: 'externalId',
      label: 'External Id',
      options: {
        display: false,
        filter: false
      }
    },
    {
      name: 'retries',
      label: 'Retries',
      options: {
        filter: false
      }
    },
    {
      name: 'started',
      label: 'Started',
      options: {
        customBodyRender: renderTimestamp,
        display: false,
        filter: false
      }
    },
    {
      name: 'created',
      label: 'Created',
      options: {
        customBodyRender: renderTimestamp,
        display: false,
        filter: false
      }
    },
    {
      name: 'modified',
      label: 'Last modified',
      options: {
        customBodyRender: renderTimestamp,
        filter: false
      }
    },
    {
      name: 'nextActivation',
      label: 'Next activation',
      options: {
        customBodyRender: renderTimestamp,
        filter: false
      }
    },
    {
      name: 'priority',
      label: 'Priority',
      options: {
        display: false
      }
    }
  ]);

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

  const components = {
    ExpandButton: function (props: any) {
      if (
        instances &&
        instances[props.dataIndex] &&
        !instances[props.dataIndex].childWorkflows
      ) {
        return <div style={{width: '24px'}} />;
      }
      return <ExpandButton {...props} />;
    }
  };

  return (
    <MuiThemeProvider theme={getMuiTheme()}>
      <MUIDataTable
        title="Search result"
        data={instances}
        columns={columns}
        components={components}
        options={{
          selectableRows: 'none',
          expandableRows: true,
          expandableRowsHeader: false,
          enableNestedDataAccess: '.',
          renderExpandableRow: (rowData, rowMeta) => {
            const colSpan = rowData.length + 1;
            return (
              <TableRow>
                <TableCell colSpan={colSpan}>
                  <ChildWorkflowTable
                    workflowId={instances[rowMeta.dataIndex].id}
                  />
                </TableCell>
              </TableRow>
            );
          },
          setRowProps: (row, dataIndex, rowIndex) => {
            const rowClassName = rowClassRender(instances[dataIndex].status);
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
      data['include'] = 'childWorkflows,currentStateVariables'; // TODO: add checkbox to form
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

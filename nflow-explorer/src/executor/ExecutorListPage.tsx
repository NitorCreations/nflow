import React, {useState, useEffect, useCallback} from 'react';
import {addDays, addHours} from 'date-fns';
import {
  createTheme,
  Grid,
  Container,
  MuiThemeProvider
} from '@material-ui/core';
import MUIDataTable from 'mui-datatables';

import {formatRelativeTime, formatTimestamp} from '../utils';
import {useConfig} from '../config';
import {Spinner} from '../component';
import {Executor} from '../types';
import {listAllExecutors} from '../service';

const ExecutorTable = ({executors}: {executors: Executor[]}) => {
  const getMuiTheme = () =>
    createTheme({
      overrides: {
        MUIDataTable: {
          root: {},
          paper: {
            boxShadow: 'none'
          }
        },
        MUIDataTableBodyCell: {
          root: {
            padding: 6,
            wordBreak: 'break-all'
          }
        },
        MUIDataTableToolbar: {
          root: {
            display: 'none'
          }
        }
      }
    });

  const renderTimestamp = (value: string) => {
    return (
      <div title={formatRelativeTime(value)}>{formatTimestamp(value)}</div>
    );
  };

  const columns = [
    {name: 'id', label: 'ID'},
    {name: 'host', label: 'Host'},
    {name: 'pid', label: 'Process ID'},
    {name: 'executorGroup', label: 'Executor Group'},
    {
      name: 'started',
      label: 'Started',
      options: {
        customBodyRender: renderTimestamp
      }
    },
    {
      name: 'stopped',
      label: 'Stopped',
      options: {
        customBodyRender: renderTimestamp
      }
    },
    {
      name: 'active',
      label: 'Activity hearbeat',
      options: {
        customBodyRender: renderTimestamp
      }
    },
    {
      name: 'expires',
      label: 'Hearbeat expires',
      options: {
        customBodyRender: renderTimestamp
      }
    }
  ];

  const rowClassRender = (executor: any) => {
    const now = new Date();
    if (executor.stopped) {
      return '';
    }
    if (!executor.expires) {
      // has never been active yet
      if (addDays(executor.started, 1) < now) {
        return ''; // dead
      }
      if (addHours(executor.started, 1) < now) {
        return 'warning'; // expired
      }
      return 'success'; // alive
    }
    // has been active at some point
    if (addDays(executor.active, 1) < now) {
      return ''; // dead
    }
    if (executor.expires < now) {
      return 'warning'; // expiry date is in past
    }
    return 'success'; // alive
  };

  return (
    <MuiThemeProvider theme={getMuiTheme()}>
      <MUIDataTable
        title={undefined}
        data={executors}
        columns={columns}
        options={
          {
            storageKey: 'workflowExecutorTableState',
            selectableRows: 'none',
            expandableRowsHeader: false,
            textLabels: {
              body: {
                noMatch: 'No workflow executors found'
              }
            },
            setRowProps: (_row: any, dataIndex: any, _rowIndex: any) => {
              const rowClassName = rowClassRender(executors[dataIndex]);
              return {
                className: `${rowClassName}`
              };
            },
            setTableProps: () => {
              return {
                className: 'table table-hover'
              };
            }
          } as any
        } // TODO: types do not support storageKey property yet
      />
    </MuiThemeProvider>
  );
};

function ExecutorListPage() {
  const [initialLoad, setInitialLoad] = useState<boolean>(true);
  const [executors, setExecutors] = useState<Array<Executor>>([]);
  const config = useConfig();

  const fetchExecutors = useCallback(() => {
    listAllExecutors(config)
      .then(data => setExecutors(data))
      .catch(error => {
        // TODO error handling
        console.error('Error', error);
      })
      .finally(() => setInitialLoad(false));
  }, [config]);

  // refresh list periodically
  useEffect(() => {
    fetchExecutors();
    let handle = setInterval(fetchExecutors, config.refreshSeconds * 1000);
    return () => clearInterval(handle);
  }, [config.refreshSeconds, fetchExecutors]);

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        {initialLoad ? (
          <Container>
            <Spinner />
          </Container>
        ) : (
          <ExecutorTable executors={executors} />
        )}
      </Grid>
    </Grid>
  );
}

export default ExecutorListPage;

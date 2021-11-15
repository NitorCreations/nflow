import React, {useState, useEffect, useCallback} from 'react';
import {addDays, addHours} from 'date-fns';
import {Typography, Grid, Container} from '@material-ui/core';

import {formatRelativeTime, formatTimestamp} from '../utils';
import {useConfig} from '../config';
import {DataTable, Spinner} from '../component';
import {Executor} from '../types';
import {listExecutors} from '../service';

const ExecutorTable = ({executors}: {executors: Executor[]}) => {
  const columns = [
    {field: 'id', headerName: 'ID'},
    {field: 'host', headerName: 'Host'},
    {field: 'pid', headerName: 'Process ID'},
    {field: 'executorGroup', headerName: 'Executor Group'},
    {
      field: 'started',
      headerName: 'Started',
      fieldRender: formatRelativeTime,
      tooltipRender: formatTimestamp
    },
    {
      field: 'stopped',
      headerName: 'Stopped',
      fieldRender: formatRelativeTime,
      tooltipRender: formatTimestamp
    },
    {
      field: 'active',
      headerName: 'Activity hearbeat',
      fieldRender: formatRelativeTime,
      tooltipRender: formatTimestamp
    },
    {
      field: 'expires',
      headerName: 'Hearbeat expires',
      fieldRender: formatRelativeTime,
      tooltipRender: formatTimestamp
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
      return 'successs'; // alice
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
    <DataTable
      rows={executors}
      columns={columns}
      rowClassRender={rowClassRender}
    />
  );
};

function ExecutorListPage() {
  const [initialLoad, setInitialLoad] = useState<boolean>(true);
  const [executors, setExecutors] = useState<Array<Executor>>([]);
  const config = useConfig();

  const fetchExecutors = useCallback(() => {
    listExecutors(config)
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

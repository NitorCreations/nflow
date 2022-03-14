import React, {useState, useEffect, useCallback} from 'react';
import {
  createTheme,
  Grid,
  Container,
  MuiThemeProvider
} from '@material-ui/core';
import MUIDataTable from 'mui-datatables';
import AddCircleOutlineOutlinedIcon from '@material-ui/icons/AddCircleOutlineOutlined';
import Search from '@material-ui/icons/Search';

import {useConfig} from '../config';
import {InternalLink, Spinner} from '../component';
import {WorkflowDefinition} from '../types';
import {listWorkflowDefinitions} from '../service';

const DefinitionTable = ({
  definitions
}: {
  definitions: WorkflowDefinition[];
}) => {
  const getMuiTheme = () =>
    createTheme({
      overrides: {
        MUIDataTable: {
          root: {},
          paper: {
            boxShadow: 'none'
          }
        },
        MUIDataTableBodyRow: {
          root: {
            '&:nth-child(odd)': {
              backgroundColor: '#efefef'
            }
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

  const linkRender = (type: string) => {
    const path = '/workflow-definition/' + type;
    return <InternalLink to={path}>{type}</InternalLink>;
  };

  const columns = [
    {
      name: 'type',
      label: 'Type',
      options: {
        customBodyRender: (value: any) => linkRender(value)
      }
    },
    {
      name: 'description',
      label: 'Description'
    },
    {
      name: 'type',
      label: 'Manage instances',
      options: {
        sort: false,
        customBodyRender: (value: any) => (
          <div>
            <InternalLink to={`/workflow/create?type=${value}`}>
              <AddCircleOutlineOutlinedIcon />
            </InternalLink>
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <InternalLink to={`/workflow?type=${value}`}>
              <Search />
            </InternalLink>
          </div>
        )
      }
    }
  ];

  return (
    <MuiThemeProvider theme={getMuiTheme()}>
      <MUIDataTable
        title={undefined}
        data={definitions}
        columns={columns}
        options={
          {
            storageKey: 'workflowDefinitionsTableState',
            selectableRows: 'none',
            expandableRowsHeader: false,
            textLabels: {
              body: {
                noMatch: 'No workflow definitions found'
              }
            }
          } as any
        } // TODO: types do not support storageKey property yet
      />
    </MuiThemeProvider>
  );
};

function WorkflowDefinitionListPage() {
  const config = useConfig();

  const [initialLoad, setInitialLoad] = useState<boolean>(true);
  const [definitions, setDefinitions] = useState<Array<WorkflowDefinition>>([]);

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

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        {initialLoad ? (
          <Container>
            <Spinner />
          </Container>
        ) : (
          <DefinitionTable definitions={definitions} />
        )}
      </Grid>
    </Grid>
  );
}

export default WorkflowDefinitionListPage;

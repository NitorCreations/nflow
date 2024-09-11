import React, {useState, useEffect, useCallback} from 'react';
import {
  createTheme,
  Grid,
  Container,
  ThemeProvider
} from '@mui/material';
import MUIDataTable from 'mui-datatables';
import {AddCircleOutlineOutlined as AddCircleOutlineOutlinedIcon, Search} from '@mui/icons-material';

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
      components: {
        MUIDataTable: {
          styleOverrides: {
            root: {},
            paper: {
              boxShadow: 'none'
            }
          }
        },
        MUIDataTableBodyRow: {
          styleOverrides: {
            root: {
              '&:nth-child(odd)': {
                backgroundColor: '#efefef'
              }
            }
          }
        },
        MUIDataTableBodyCell: {
          styleOverrides: {
            root: {
              padding: 6,
              wordBreak: 'break-all'
            }
          }
        },
        MUIDataTableToolbar: {
          styleOverrides: {
            root: {
              display: 'none'
            }
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
    <ThemeProvider theme={getMuiTheme()}>
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
          }
        }
      />
    </ThemeProvider>
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

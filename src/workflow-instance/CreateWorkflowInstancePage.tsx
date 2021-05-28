import React, { useEffect, useState, useContext } from "react";
import { Typography, Grid, Container } from '@material-ui/core';

import { CreateWorkflowInstanceForm } from "./CreateWorkflowInstanceForm";
import { WorkflowDefinition } from "../types";
import { ConfigContext } from "../config";
import { Spinner } from "../component";
import { listWorkflowDefinitions } from "../service";

function CreateWorkflowInstancePage() {
    const config = useContext(ConfigContext);

    const [definitions, setDefinitions] = useState<WorkflowDefinition[]>()

    useEffect(() => {
        listWorkflowDefinitions(config)
            .then(setDefinitions)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    const showForm = (definitions: WorkflowDefinition[]) => {
        return (        
            <Grid container spacing={3}>
                <Grid item xs={12}>
                    <Container>
                        <Typography variant="h2">Create a new workflow instance</Typography>
                        <CreateWorkflowInstanceForm definitions={definitions} />
                    </Container>
                </Grid>
            </Grid>
        );
    }
    if (definitions) {
        return showForm(definitions);
    }
    return (
        <Grid container spacing={3}>
            <Grid item xs={12}>
                <Container>
                    <Spinner />    
                </Container>
            </Grid>
        </Grid>
    );  
}

export { CreateWorkflowInstancePage };

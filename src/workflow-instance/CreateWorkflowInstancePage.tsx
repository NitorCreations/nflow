import React, { useEffect, useState, useContext, Fragment } from "react";
import { Typography } from "@material-ui/core";

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
        return (<div>
            <Typography variant="h2">Create a new workflow instance</Typography>
            <CreateWorkflowInstanceForm definitions={definitions} />
        </div>)
    }
    if (definitions) {
        return showForm(definitions);
    }
    return <Spinner />
}

export { CreateWorkflowInstancePage };

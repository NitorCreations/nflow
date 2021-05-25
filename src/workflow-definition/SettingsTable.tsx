import { WorkflowDefinition } from "../types";

const SettingsTable = (props: {definition: WorkflowDefinition}) => {
    const settings = props.definition.settings;
    return (
        <table>
            <thead>
                <tr>
                    <th>Setting</th>
                    <th>Value</th>
                </tr>
            </thead>
            <tbody>
                {Object.keys(settings).map(key => (
                    <tr id={key}>
                        <td>{key}</td>
                        <td><pre><code>
                            {JSON.stringify(settings[key], null, 2)}
                        </code></pre></td>
                    </tr>
                ))}
            </tbody>
        </table>
    )
};

export { SettingsTable };
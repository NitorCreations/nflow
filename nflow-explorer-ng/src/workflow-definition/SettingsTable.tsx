import {WorkflowDefinition} from '../types';
import {DataTable} from '../component';

const SettingsTable = (props: {definition: WorkflowDefinition}) => {
  const settings = props.definition.settings;
  const columns = [
    {field: 'key', headerName: 'Setting'},
    {
      field: 'value',
      headerName: 'Value',
      fieldRender: (value: any) => (
        <pre>
          <code>{value}</code>
        </pre>
      )
    }
  ];
  const rows: any[] = Object.keys(settings).map(key => ({
    key: key,
    value: JSON.stringify(settings[key], null, 2)
  }));
  return <DataTable rows={rows} columns={columns} />;
};

export {SettingsTable};

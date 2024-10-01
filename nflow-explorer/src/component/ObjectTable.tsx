import React from 'react';
import {
  Paper,
  Table,
  TableBody,
  TableRow,
  TableCell,
  TableContainer
} from '@mui/material';

// https://material-ui.com/components/tables/
interface Column {
  field: string;
  headerName: string;
  align?: 'inherit' | 'left' | 'center' | 'right' | 'justify';
  fieldRender?: (value: any) => any;
  rowRender?: (value: any) => any;
  tooltipRender?: (value: any) => string | undefined;
}

const BodyCell = ({
  object,
  column,
  index,
  valueClassRender
}: {
  object: any;
  column: Column;
  index: number;
  valueClassRender: (column: any, object: any) => string | undefined;
}) => {
  let value = object[column.field];
  if (column.fieldRender) {
    value = column.fieldRender(object[column.field]);
  }
  if (column.rowRender) {
    value = column.rowRender(object);
  }

  let tooltip = undefined;
  if (column.tooltipRender) {
    tooltip = column.tooltipRender(object[column.field]);
  }
  return (
    <TableCell
      key={index}
      title={tooltip}
      className={valueClassRender(column, object)}
      align={column?.align || 'left'}
    >
      {value}
    </TableCell>
  );
};

/**
 * Shows a single object in a table
 */
function ObjectTable(props: {
  object: any;
  columns: Column[];
  valueClassRender?: (column: any, object: any) => string | undefined;
}) {
  const valueClassRender =
    props.valueClassRender || ((column: any, object: any) => '');

  return (
    <TableContainer component={Paper}>
      <Table
        aria-label="simple table"
        size="small"
        className="table table-striped table-hover"
      >
        <TableBody>
          {props.columns.map(
            (column, index) =>
              column.field in props.object && (
                <TableRow key={index}>
                  <TableCell>{column.headerName}</TableCell>
                  <BodyCell
                    object={props.object}
                    column={column}
                    index={index}
                    valueClassRender={valueClassRender}
                  />
                </TableRow>
              )
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

export {ObjectTable};

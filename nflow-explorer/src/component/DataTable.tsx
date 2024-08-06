import React from 'react';
import {
  Paper,
  Table,
  TableHead,
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
  row,
  column,
  index
}: {
  row: any;
  column: Column;
  index: number;
}) => {
  let value = row[column.field];
  if (column.fieldRender) {
    value = column.fieldRender(row[column.field]);
  }
  if (column.rowRender) {
    value = column.rowRender(row);
  }
  let tooltip = undefined;
  if (column.tooltipRender) {
    tooltip = column.tooltipRender(row[column.field]);
  }
  return (
    <TableCell title={tooltip} key={index} align={column?.align || 'left'}>
      {value}
    </TableCell>
  );
};

const Body = ({
  rows,
  columns,
  rowClassRender
}: {
  rows: any[];
  columns: Column[];
  rowClassRender: (value: any) => string | undefined;
}) => {
  return (
    <TableBody>
      {rows.map((row, index) => (
        <TableRow key={index} className={rowClassRender(row)}>
          {columns.map((column, index) => (
            <BodyCell key={index} column={column} row={row} index={index} />
          ))}
        </TableRow>
      ))}
    </TableBody>
  );
};

/**
 * Shows a list of objects in a table.
 */
function DataTable(props: {
  rows: any[];
  columns: Column[];
  rowClassRender?: (value: any) => string | undefined;
}) {
  const {rows, columns, rowClassRender = (x: any) => ''} = props;

  const header = (
    <TableHead>
      <TableRow>
        {props.columns.map((column, index) => (
          <TableCell key={index} align={column.align}>
            {column.headerName}
          </TableCell>
        ))}
      </TableRow>
    </TableHead>
  );

  return (
    <TableContainer component={Paper}>
      <Table
        aria-label="simple table"
        size="small"
        className="table table-striped table-hover"
      >
        {header}
        <Body rows={rows} columns={columns} rowClassRender={rowClassRender} />
      </Table>
    </TableContainer>
  );
}

export {DataTable};

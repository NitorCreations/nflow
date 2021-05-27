import React from "react";
import { Paper, Table, TableHead, TableBody, TableRow, TableCell, TableContainer } from '@material-ui/core';

// https://material-ui.com/components/tables/
interface Column {
    field: string;
    headerName: string;
    align?: "inherit" | "left" | "center" | "right" | "justify";
    fieldRender?: (value: any) => any;
    rowRender?: (value: any) => any;
    tooltipRender?: (value: any) => string | undefined;
};

/**
 * Shows a list of objects in a table.
 */
function DataTable(props: {rows: any[], columns: Column[], rowClassRender?: (value: any) => string | undefined}) {
    const header = () => {
        return (
        <TableHead>
            <TableRow>
                {props.columns.map((column, index) => (
                    <TableCell key={index} align={column.align}>{column.headerName}</TableCell>
                ))}
            </TableRow>
        </TableHead>
        )
    };

    const bodyCell = (row: any, column: Column, index: number) => {
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
        return (<TableCell title={tooltip} key={index} align={column?.align || 'left'}>{value}</TableCell>)
    };

    const body = () => {
        const rowClassRender = props.rowClassRender || ((x:any) => "");
        return (
            <TableBody>
                {props.rows.map((row, index) => (
                    <TableRow key={index} className={rowClassRender(row)}>
                        {props.columns.map((column, index) => (
                            bodyCell(row, column, index)
                        ))}
                    </TableRow>
                ))}
            </TableBody>
        )
    };

    return (
        <TableContainer component={Paper}>
            <Table aria-label="simple table" size="small" className="table table-striped table-hover">
                {header()}
                {body()}
            </Table>
        </TableContainer>
    );
};

export { DataTable }

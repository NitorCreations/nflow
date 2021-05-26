import React from "react";
import { Paper, Table, TableBody, TableRow, TableCell, TableContainer } from '@material-ui/core';

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
 * Shows a single object in a table
 */
function ObjectTable(props: {object: any, columns: Column[]}) {

    const bodyCell = (object: any, column: Column, index: number) => {
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
        return (<TableCell title={tooltip} key={index} align={column?.align || 'left'}>{value}</TableCell>)
    };

    const body = () => {
        return (
            <TableBody>
                {props.columns.map((column, index) => (
                    props.object[column.field] &&
                    <TableRow key={index}>
                        <TableCell>{column.headerName}</TableCell>
                        {bodyCell(props.object, column, index)}
                    </TableRow>
                ))}
            </TableBody>
        )
    };

    return (
        <TableContainer component={Paper}>
            <Table aria-label="simple table" size="small" className="table-striped table-hover">
                {body()}
            </Table>
        </TableContainer>
    );
};

export { ObjectTable }

import React from "react";

import Select from '@material-ui/core/Select';
import FormControl from '@material-ui/core/FormControl';
import InputLabel from '@material-ui/core/InputLabel';
import MenuItem from '@material-ui/core/MenuItem';

// TODO check if this index business is needed?
let index = 0;
function Selection(props: {
    label: string,
    items: string[],
    selected: string,
    onChange: (v: string) => any,
    getSelectionLabel: (v: string) => any,
}) {
    // TODO https://github.com/mui-org/material-ui/issues/13394
    //      will log ugly warning in dev first time the popup is opened
    //      Material-UI v5 should fix this. Does not appear in prod.
    let currentIndex = ++index;
    return (
        <FormControl>
            <InputLabel id={"select-label-" + currentIndex}>{props.label}</InputLabel>
            <Select
                labelId={"select-label-" + currentIndex}
                id={"selection-" + currentIndex}
                value={props.selected}
                onChange={(e: any) => props.onChange(e.target.value)}
            >
                {props.items.map((item) => (
                    <MenuItem key={item} value={item}>
                        {props.getSelectionLabel(item)}
                    </MenuItem>
                ))}
            </Select>
        </FormControl>
    );
};

export { Selection };

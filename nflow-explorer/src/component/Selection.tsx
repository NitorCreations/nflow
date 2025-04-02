import React from 'react';

import {Select, FormControl, InputLabel, MenuItem, Autocomplete, TextField} from '@mui/material';

// TODO check if this index business is needed?
let index = 0;
function Selection(props: {
  label: string;
  items: string[];
  selected: string;
  onChange: (v: string) => any;
  getSelectionLabel: (v: string) => any;
}) {
  let currentIndex = ++index;

    // Sort items alphabetically by their display label
    const sortedItems = [...props.items].sort((a, b) => {
        const labelA = props.getSelectionLabel(a).toString().toLowerCase();
        const labelB = props.getSelectionLabel(b).toString().toLowerCase();
        return labelA.localeCompare(labelB);
    });

  return (
    <FormControl style={{minWidth: 240}} variant="standard">
            <Autocomplete
                options={sortedItems}
                value={props.selected}
                onChange={(_, newValue: string | null) => props.onChange(newValue || '')}
                renderInput={(params) => (
                    <TextField
                        {...params}
                        label={props.label}
                        variant="standard"
                    />
                )}
                getOptionLabel={(option: string) => props.getSelectionLabel(option)}
                renderOption={(itemProps, option: string) => (
                    <li {...itemProps}>
                        {props.getSelectionLabel(option)}
                    </li>
                )}
                disablePortal
                fullWidth
            />
    </FormControl>
  );
}

export {Selection};

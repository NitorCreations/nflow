import React from 'react';

import {Select, FormControl, InputLabel, MenuItem} from '@mui/material';

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
  return (
    <FormControl style={{minWidth: 240}} variant="standard">
      <InputLabel id={`select-label-${currentIndex}`}>{props.label}</InputLabel>
      <Select
        labelId={`select-label-${currentIndex}`}
        id={`selection-${currentIndex}`}
        value={props.selected}
        onChange={(e: any) => props.onChange(e.target.value)}
      >
        {props.items.map(item => (
          <MenuItem key={item} value={item}>
            {props.getSelectionLabel(item)}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

export {Selection};

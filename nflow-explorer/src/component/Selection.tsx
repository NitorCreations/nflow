import React from 'react';

import {Select, FormControl, InputLabel, MenuItem} from '@material-ui/core';
import {makeStyles} from '@material-ui/core/styles';

const useStyles = makeStyles(theme => ({
  root: {
    margin: theme.spacing(1)
  }
}));

// TODO check if this index business is needed?
let index = 0;
function Selection(props: {
  label: string;
  items: string[];
  selected: string;
  onChange: (v: string) => any;
  getSelectionLabel: (v: string) => any;
}) {
  // TODO https://github.com/mui-org/material-ui/issues/13394
  //      will log ugly warning in dev first time the popup is opened
  //      Material-UI v5 should fix this. Does not appear in prod.
  const classes = useStyles();

  let currentIndex = ++index;
  return (
    <FormControl className={classes.root} style={{minWidth: 200}}>
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

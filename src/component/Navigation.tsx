import React from 'react';
import {NavLink} from 'react-router-dom';
import Typography from '@material-ui/core/Typography';

import './Navigation.scss';
import {AppBar, Button, Toolbar} from '@material-ui/core';
import {createStyles, makeStyles, Theme} from '@material-ui/core/styles';

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    activeLink: {
      background: 'white',
      color: 'blue!important'
    },
    passiveLink: {
      color: 'white',
      fontSize: 'large'
    }
  })
);

const Navigation = () => {
  const classes = useStyles();
  return (
    <AppBar position="static">
      <Toolbar>
        <Typography variant="h4">nFlow</Typography>
        <Button
          component={NavLink}
          to="/"
          activeClassName={classes.activeLink}
          className={classes.passiveLink}
          exact={true}
        >
          Workflow definitions
        </Button>
        <Button
          component={NavLink}
          to="/workflow"
          activeClassName={classes.activeLink}
          className={classes.passiveLink}
        >
          Workflow instances
        </Button>
        <Button
          component={NavLink}
          to="/executors"
          activeClassName={classes.activeLink}
          className={classes.passiveLink}
        >
          Executors
        </Button>
        <Button
          component={NavLink}
          to="/about"
          activeClassName={classes.activeLink}
          className={classes.passiveLink}
        >
          About
        </Button>
      </Toolbar>
    </AppBar>
  );
};

export {Navigation};

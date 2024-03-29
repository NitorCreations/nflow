import React, {useState} from 'react';
import {NavLink, useHistory} from 'react-router-dom';
import Typography from '@material-ui/core/Typography';

import './Navigation.scss';
import {AppBar, Button, MenuItem, Select, Toolbar} from '@material-ui/core';
import {createStyles, makeStyles, Theme} from '@material-ui/core/styles';
import {useConfig} from '../config';
import {Config} from '../types';

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    activeLink: {
      background: 'white',
      color: 'blue!important'
    },
    passiveLink: {
      color: 'white',
      fontSize: 'large'
    },
    endpointSelect: {
      background: 'white'
    }
  })
);

const renderLogo = (config: Config) => {
  const nflowLogoTitle = config.nflowLogoTitle
    ? config.nflowLogoTitle
    : 'nFlow';
  if (config.nflowLogoFile) {
    return <img src={config.nflowLogoFile} alt={nflowLogoTitle} />;
  }
  return <Typography variant="h4">{nflowLogoTitle}</Typography>;
};

const Navigation = () => {
  const config = useConfig();
  const history = useHistory();
  const classes = useStyles();
  const [selectedEndpointId, setSelectedEndpointId] = useState(
    config.activeNflowEndpoint.id
  );
  return (
    <AppBar position="static">
      <Toolbar>
        {renderLogo(config)}
        {config.nflowEndpoints.length > 1 && (
          <Select
            value={selectedEndpointId}
            variant="outlined"
            className={classes.endpointSelect}
            onChange={selected => {
              const newActiveEndpoint = config.nflowEndpoints.find(
                endpoint => endpoint.id === selected.target.value
              );
              if (newActiveEndpoint) {
                config.activeNflowEndpoint = newActiveEndpoint;
                setSelectedEndpointId(newActiveEndpoint.id);
                history.push('/');
              }
            }}
          >
            {config.nflowEndpoints.map((endPoint, index) => {
              return (
                <MenuItem key={index} value={endPoint.id}>
                  {endPoint.title}
                </MenuItem>
              );
            })}
          </Select>
        )}
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
          to="/workflow-definition"
          activeClassName={classes.activeLink}
          className={classes.passiveLink}
        >
          Workflow definitions
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

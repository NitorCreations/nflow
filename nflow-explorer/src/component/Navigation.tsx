import React, {useState} from 'react';
import {NavLink, useNavigate} from 'react-router-dom';

import './Navigation.scss';
import {AppBar, Button, MenuItem, Select, Toolbar, Typography} from '@mui/material';
import {useConfig} from '../config';
import {Config} from '../types';

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
  const navigate = useNavigate();
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
            sx={{background: "white"}}
            onChange={selected => {
              const newActiveEndpoint = config.nflowEndpoints.find(
                endpoint => endpoint.id === selected.target.value
              );
              if (newActiveEndpoint) {
                config.activeNflowEndpoint = newActiveEndpoint;
                setSelectedEndpointId(newActiveEndpoint.id);
                navigate('/');
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
        <Button>
          <NavLink to="/workflow">Workflow instances</NavLink>
        </Button>
        <Button>
          <NavLink to="/workflow-definition">Workflow definitions</NavLink>
        </Button>
        <Button>
          <NavLink to="/executors">Executors</NavLink>
        </Button>
        <Button>
          <NavLink to="/about">About</NavLink>
        </Button>
      </Toolbar>
    </AppBar>
  );
};

export {Navigation};

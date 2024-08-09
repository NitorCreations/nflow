import React, {useState} from 'react';
import {matchPath, NavLink, useLocation, useNavigate} from 'react-router-dom';

import './Navigation.scss';
import {AppBar, Box, Button, IconButton, Menu, MenuItem, Select, Tab, Tabs, Toolbar, Typography} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
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
  const [anchorElNav, setAnchorElNav] = React.useState<null | HTMLElement>(null);

  const pages = [
    ['/workflow', 'Workflow instances'],
    ['/workflow-definition', 'Workflow definitions'],
    ['/executors', 'Executors'],
    ['/about', 'About']
  ]

  const handleOpenNavMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorElNav(event.currentTarget);
  };
  const handleCloseNavMenu = () => {
    setAnchorElNav(null);
  };

  const useRouteMatch = (patterns: readonly string[]) => {
    const { pathname } = useLocation();
    for (let i = 0; i < patterns.length; i += 1) {
      const pattern = patterns[i].replace("#", "");
      const possibleMatch = matchPath(pattern, pathname);
      if (possibleMatch !== null) {
        return possibleMatch;
      }
    }
    return null;
  }
  const routeMatch = useRouteMatch(pages.map((page) => page[0]));
  const currentTab = routeMatch?.pattern?.path;

  return (
    <AppBar position="static">
      <Toolbar>
        <div style={{display: "flex", alignItems: "center", columnGap: "1rem"}}>
          <Box sx={{ flexGrow: 1, display: { xs: 'flex', md: 'none' } }}>
            <IconButton
              size="large"
              onClick={handleOpenNavMenu}
              color="inherit"
            >
              <MenuIcon />
            </IconButton>
            <Menu
              id="menu-appbar"
              anchorEl={anchorElNav}
              anchorOrigin={{
                vertical: 'bottom',
                horizontal: 'left',
              }}
              keepMounted
              transformOrigin={{
                vertical: 'top',
                horizontal: 'left',
              }}
              open={Boolean(anchorElNav)}
              onClose={handleCloseNavMenu}
              sx={{
                display: { xs: 'block', md: 'none' },
              }}
            >
              {pages.map((page) => (
                <MenuItem key={page[0]} to={page[0]} component={NavLink} onClick={handleCloseNavMenu}>
                  <Typography textAlign="center">{page[1]}</Typography>
                </MenuItem>
              ))}
            </Menu>
          </Box>
          {renderLogo(config)}
          {config.nflowEndpoints.length > 1 && (
          <Select
            value={selectedEndpointId}
            variant="outlined"
            sx={{background: "white", height: "2rem"}}
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

        </div>
        <Tabs
          value={currentTab || "/workflow"}
          textColor="secondary"
          indicatorColor="secondary"
          sx={{display: {xs: "none", md: "flex"}}}
        >
          <Tab label="Workflow instances" value="/workflow" to="/workflow" component={NavLink} />
          <Tab label="Workflow definitions" value="/workflow-definition" to="/workflow-definition" component={NavLink} />
          <Tab label="Executors" value="/executors" to="/executors" component={NavLink} />
          <Tab label="About" value="/about" to="/about" component={NavLink} />
        </Tabs>
      </Toolbar>
    </AppBar>
  );
};

export {Navigation};

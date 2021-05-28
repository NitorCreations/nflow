import React from "react";
import { NavLink } from "react-router-dom";
import Link from '@material-ui/core/Link';
import Typography from '@material-ui/core/Typography';

import "./Navigation.css";

const Navigation = () => {
    const isActive = (regexps: RegExp[]) => (match: any, location: any) => {
        for (const regexp of regexps) {
            if (location.pathname.match(regexp)) {
                return true;
            }
        }
        return false;
    }

    return (
        <nav>
            <Typography>
                <img src="/nflow_logo.svg" 
                    style={{height: '2.5em'}}
                    alt="nFlow-logo" />
                <span style={{verticalAlign: 'top'}}>
                    <Link component={NavLink} to="/" 
                        isActive={isActive([new RegExp('^/$'), new RegExp('^/workflow-definition/.*')])}
                        activeClassName="navi-selected">Workflow definitions</Link>
                    <Link component={NavLink} to="/search" 
                        isActive={isActive([new RegExp('^/search'), new RegExp('^/workflow/.*')])} 
                        activeClassName="navi-selected">Workflow instances</Link>
                    <Link component={NavLink} to="/executors" 
                        isActive={isActive([new RegExp('^/executors')])} 
                        activeClassName="navi-selected">Executors</Link>
                    <Link component={NavLink} to="/about" 
                        isActive={isActive([new RegExp('^/about')])} activeClassName="navi-selected">About</Link>
                </span>
            </Typography>
        </nav>)};

export { Navigation };

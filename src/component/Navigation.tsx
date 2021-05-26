import React from "react";
import { NavLink } from "react-router-dom";
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

    return (<nav>
        <NavLink to="/">
            <img src="/nflow_logo.svg" alt="nFlow-logo" />
        </NavLink>
        <NavLink to="/" 
            isActive={isActive([new RegExp('^/$'), new RegExp('^/workflow-definition/.*')])}
            activeClassName="navi-selected">Workflow definitions</NavLink>
        <NavLink to="/search" 
            isActive={isActive([new RegExp('^/search'), new RegExp('^/workflow/.*')])} 
            activeClassName="navi-selected">Workflow instances</NavLink>
        <NavLink to="/executors" 
            isActive={isActive([new RegExp('^/executors')])} 
            activeClassName="navi-selected">Executors</NavLink>
        <NavLink to="/about" 
            isActive={isActive([new RegExp('^/about')])} activeClassName="navi-selected">About</NavLink>
     </nav>)};

export { Navigation };

import React from 'react'
import {NavLink} from 'react-router-dom'
import Link from '@material-ui/core/Link'
import Typography from '@material-ui/core/Typography'

import './Navigation.scss'

const isActive = (regexps: string[]) => (match: any, location: any) => {
  return !!regexps.find(regexp => location.pathname.match(regexp))
}

const Navigation = () => {
  return (
    <nav>
      <Typography>
        <img src="/nflow_logo.svg" style={{height: '2.5em'}} alt="nFlow-logo" />
        <span style={{verticalAlign: 'top'}}>
          <Link
            component={NavLink}
            to="/"
            isActive={isActive(['^/$', '^/workflow-definition/.*'])}
            activeClassName="navi-selected"
          >
            Workflow definitions
          </Link>
          <Link
            component={NavLink}
            to="/search"
            isActive={isActive(['search', '^/workflow/.*'])}
            activeClassName="navi-selected"
          >
            Workflow instances
          </Link>
          <Link
            component={NavLink}
            to="/executors"
            isActive={isActive(['executors'])}
            activeClassName="navi-selected"
          >
            Executors
          </Link>
          <Link
            component={NavLink}
            to="/about"
            isActive={isActive(['about'])}
            activeClassName="navi-selected"
          >
            About
          </Link>
        </span>
      </Typography>
    </nav>
  )
}

export {Navigation}

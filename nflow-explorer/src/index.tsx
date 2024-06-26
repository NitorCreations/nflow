import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import reportWebVitals from './reportWebVitals';
import {readConfig, ConfigContext} from './config';
import {createTheme, MuiThemeProvider} from '@material-ui/core/styles';
import {TableCell} from '@material-ui/core';

// https://material-ui.com/components/typography/#general
// https://fontsource.org/docs/getting-started
import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';
import './index.scss';
import {Config} from "./types";
import {PublicClientApplication, InteractionType} from "@azure/msal-browser";
import {
  MsalAuthenticationTemplate,
  MsalProvider,
} from "@azure/msal-react";

// see: https://github.com/gregnb/mui-datatables/issues/1893
const oldRender = (TableCell as any).render;
(TableCell as any).render = function (...args: any) {
  const [props, ...otherArgs] = args;
  if (typeof props === 'object' && props && 'isEmpty' in props) {
    const {isEmpty, ...propsWithoutEmpty} = props;
    return oldRender.apply(this, [propsWithoutEmpty, ...otherArgs]);
  } else {
    return oldRender.apply(this, args);
  }
};

const theme = createTheme({
  palette: {
    primary: {
      light: '#6DC4E2',
      main: '#6056EB',
      dark: '#26273A',
      contrastText: '#ffffff'
    }
  }
});

const wrapMsalIfNeeded = (app: any, config: Config): any => {
  if (config.msalConfig) {
    console.info('Initializing MSAL')
    const authRequest = {
      scopes: ["openid"]
    };
    config.msalClient = new PublicClientApplication(config.msalConfig);
    return (
      <MsalProvider instance={config.msalClient}>
        <MsalAuthenticationTemplate interactionType={InteractionType.Redirect} authenticationRequest={authRequest}>
          {app}
        </MsalAuthenticationTemplate>
      </MsalProvider>
    )
  }
  return app;
}

readConfig().then(config => {
  console.info('Config read');
  ReactDOM.render(
    <React.StrictMode>
      <MuiThemeProvider theme={theme}>
        <ConfigContext.Provider value={config}>
          {wrapMsalIfNeeded(<App />, config)}
        </ConfigContext.Provider>
      </MuiThemeProvider>
    </React.StrictMode>,
    document.getElementById('root')
  );
});

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();

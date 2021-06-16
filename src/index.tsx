import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import reportWebVitals from './reportWebVitals';
import {readConfig, ConfigContext} from './config';
import {createMuiTheme, MuiThemeProvider} from '@material-ui/core/styles';

// https://material-ui.com/components/typography/#general
// https://fontsource.org/docs/getting-started
import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';
import './index.scss';

const theme = createMuiTheme({
  palette: {
    primary: {
      light: '#6DC4E2',
      main: '#6056EB',
      dark: '#26273A',
      contrastText: '#ffffff'
    }
  }
});

readConfig('/config.json').then(config => {
  console.info('Config read');
  ReactDOM.render(
    <React.StrictMode>
      <MuiThemeProvider theme={theme}>
        <ConfigContext.Provider value={config}>
          <App />
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

import React, {useContext} from 'react';
import {Config} from './types';

// TODO read config file from somewhere
const config: Config = {
  baseUrl: 'https://bank.nflow.io/nflow',
  refreshSeconds: 60
};

const ConfigContext = React.createContext<Config>(config);

const readConfig = (configUrl: string) => {
  console.info('Read config from', configUrl);
  return Promise.resolve(config);
};

const useConfig = (): Config => {
  return useContext(ConfigContext);
};

// TODO config.js in the old explorer supported also arbitrary code?

export {readConfig, ConfigContext, useConfig};

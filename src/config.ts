import React, {useContext} from 'react';
import {Config} from './types';

declare global {
  interface Window {
    Config: Config;
  }
}

const config = window.Config;

const ConfigContext = React.createContext<Config>(config);

const readConfig = (configUrl: string) => {
  console.info('Read config from', config);

  return Promise.resolve(config);
};

const useConfig = (): Config => {
  return useContext(ConfigContext);
};

export {readConfig, ConfigContext, useConfig};

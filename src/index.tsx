import React from "react";
import ReactDOM from "react-dom";
import App from "./App";
import reportWebVitals from "./reportWebVitals";
import { readConfig, ConfigContext } from "./config";

// https://material-ui.com/components/typography/#general
// https://fontsource.org/docs/getting-started
import "@fontsource/roboto/300.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";
import "./index.css";

readConfig("/config.json").then((config) => {
  console.info("Config read");
  ReactDOM.render(
    <React.StrictMode>
      <ConfigContext.Provider value={config}>
        <App />
      </ConfigContext.Provider>
    </React.StrictMode>,
    document.getElementById("root")
  );
});

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();

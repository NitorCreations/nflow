import React from "react";
import ReactDOM from "react-dom";
import "./index.css";
import App from "./App";
import reportWebVitals from "./reportWebVitals";
import { readConfig, ConfigContext } from "./config";

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

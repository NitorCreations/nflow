import React, { useContext } from "react";
import { ConfigContext } from "../config";

function AboutPage() {
  const config = useContext(ConfigContext);

  let apiUrl = config.baseUrl + "/api";
  let docUrl = config.baseUrl + "/ui/doc/";
  return (
    <div>
      <h1>nFlow Explorer</h1>
      <p>
        See <a href="https://nflow.io/">nflow.io</a> for more details about
        nFlow.
      </p>
      <p>
        For support, please send a message to nFlow{" "}
        <a href="https://groups.google.com/forum/#!forum/nflow-users">
          mailing list
        </a>
        .
      </p>
      <p>
        nFlow development happens in{" "}
        <a href="https://github.com/NitorCreations/nflow">GitHub</a>.
      </p>

      <h2>Settings</h2>
      <p>
        This nFlow Explorer instance uses the nFlow API running at{" "}
        <a href={apiUrl}>{apiUrl}</a>.
      </p>
      <p>
        nFlow API documentation is available at <a href={docUrl}>{docUrl}</a>.
      </p>
    </div>
  );
}

export default AboutPage;

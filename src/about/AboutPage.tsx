import React, { useContext } from "react";
import { ConfigContext } from "../config";
import Typography from '@material-ui/core/Typography';

function AboutPage() {
  const config = useContext(ConfigContext);

  let apiUrl = config.baseUrl + "/api";
  let docUrl = config.baseUrl + "/ui/doc/";
  return (
    <div>
      <Typography variant="h2">
          nFlow Explorer
      </Typography>
      <Typography variant="body1">
        See <a href="https://nflow.io/">nflow.io</a> for more details about
        nFlow.
        <p />
        For support, please send a message to nFlow{" "}
        <a href="https://groups.google.com/forum/#!forum/nflow-users">
          mailing list
        </a>.
        <p />
        nFlow development happens in{" "}
        <a href="https://github.com/NitorCreations/nflow">GitHub</a>.
      </Typography>

      <Typography variant="h3">Settings</Typography>
      <Typography variant="body1">
        This nFlow Explorer instance uses the nFlow API running at{" "}
        <a href={apiUrl}>{apiUrl}</a>.
        <p />
        nFlow API documentation is available at <a href={docUrl}>{docUrl}</a>.
      </Typography>
    </div>
  );
}

export default AboutPage;

import React, { useContext } from "react";
import { ConfigContext } from "../config";
import Typography from '@material-ui/core/Typography';
import Link from '@material-ui/core/Link';

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
        See <Link href="https://nflow.io/">nflow.io</Link> for more details about
        nFlow.
        For support, please send a message to nFlow{" "}
        <Link href="https://groups.google.com/forum/#!forum/nflow-users">
          mailing list
        </Link>.
        nFlow development happens in{" "}
        <Link href="https://github.com/NitorCreations/nflow">GitHub</Link>.
      </Typography>

      <Typography variant="h3">Settings</Typography>
      <Typography variant="body1">
        This nFlow Explorer instance uses the nFlow API running at{" "}
        <Link href={apiUrl}>{apiUrl}</Link>.
      </Typography>
      <Typography variant="body1">
        nFlow API documentation is available at <Link href={docUrl}>{docUrl}</Link>.
      </Typography>
    </div>
  );
}

export default AboutPage;

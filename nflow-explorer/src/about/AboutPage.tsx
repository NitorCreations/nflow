import React from 'react';
import {useConfig} from '../config';
import {Typography, Grid, Container} from '@mui/material';
import Link from '@mui/material/Link';

function AboutPage() {
  const config = useConfig();

  let apiUrl = config.activeNflowEndpoint.apiUrl;
  let docUrl = config.activeNflowEndpoint.docUrl;
  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Container>
          <Typography variant="h3" gutterBottom>
            nFlow Explorer
          </Typography>
          <Typography variant="body1" gutterBottom>
            See <Link href="https://nflow.io/">nflow.io</Link> for more details
            about nFlow. For support, please send a message to nFlow{' '}
            <Link href="https://groups.google.com/forum/#!forum/nflow-users">
              mailing list
            </Link>
            . nFlow development happens in{' '}
            <Link href="https://github.com/NitorCreations/nflow">GitHub</Link>.
          </Typography>

          <Typography variant="h5" gutterBottom>
            Settings
          </Typography>
          <Typography variant="body1">
            This nFlow Explorer instance uses the nFlow API running at{' '}
            <Link href={apiUrl}>{apiUrl}</Link>.
          </Typography>
          {config.activeNflowEndpoint.docUrl && (
            <Typography variant="body1">
              nFlow API documentation is available at{' '}
              <Link href={docUrl}>{docUrl}</Link>.
            </Typography>
          )}
        </Container>
      </Grid>
    </Grid>
  );
}

export default AboutPage;

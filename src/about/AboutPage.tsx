import React from 'react';
import {useConfig} from '../config';
import {Typography, Grid, Container} from '@material-ui/core';
import Link from '@material-ui/core/Link';

function AboutPage() {
  const config = useConfig();

  let apiUrl = config.baseUrl + '/api';
  let docUrl = config.baseUrl + '/ui/doc/';
  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Container>
          <Typography variant="h2" gutterBottom>
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

          <Typography variant="h3" gutterBottom>
            Settings
          </Typography>
          <Typography variant="body1">
            This nFlow Explorer instance uses the nFlow API running at{' '}
            <Link href={apiUrl}>{apiUrl}</Link>.
          </Typography>
          <Typography variant="body1">
            nFlow API documentation is available at{' '}
            <Link href={docUrl}>{docUrl}</Link>.
          </Typography>
        </Container>
      </Grid>
    </Grid>
  );
}

export default AboutPage;

import React from "react";
import { Typography, Grid, Container } from '@material-ui/core';

function NotFoundPage() {
  return (
    <Grid container spacing={3}>
    <Grid item xs={12}>
      <Container>
      <Typography variant="h2">Not found</Typography>
    </Container>
    </Grid>
    </Grid>
  );
}

export default NotFoundPage;

import React, {useState} from 'react';
import {Grid, Link} from '@mui/material';
import {useLocation} from 'react-router-dom';

/*
  TODO: old Explorer (=AngularJS) takes return link-information before hashbang e.g.
  http://localhost:3000/?returnUrl=https://www.hs.fi&returnUrlLabel=Return%20to%20application%20X#!/workflow
  
  React Router wants the return link parameters after hashbang e.g.
  http://localhost:3000/#!/workflow?returnUrl=https://www.hs.fi&returnUrlLabel=Return%20to%20application%20X

  Find out if the old way can be preserved, or document the change to migration instructions.
*/
const ReturnLink = (props: any) => {
  const [search] = useState(useLocation().search);
  const returnUrl = new URLSearchParams(search).get('returnUrl');
  const returnUrlLabel = new URLSearchParams(search).get('returnUrlLabel');

  return returnUrl ? (
    <Grid container alignItems="flex-end" justifyContent="flex-end">
      <Grid item>
        <Link href={returnUrl}>
          {returnUrlLabel ? returnUrlLabel : 'Return'}
        </Link>
      </Grid>
    </Grid>
  ) : (
    <div />
  );
};

export {ReturnLink};

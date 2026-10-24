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
// Only plain web URLs may be used as a return link. Anything else (javascript:,
// data:, vbscript:, ...) rendered into an anchor href would let a crafted link
// execute script in the Explorer's origin when the operator clicks it.
const toSafeReturnUrl = (value: string | null): string | null => {
  if (!value) {
    return null;
  }
  try {
    const base = typeof window !== 'undefined' ? window.location.href : undefined;
    const url = new URL(value, base);
    return url.protocol === 'http:' || url.protocol === 'https:' ? value : null;
  } catch {
    return null;
  }
};

const ReturnLink = (props: any) => {
  const [search] = useState(useLocation().search);
  const returnUrl = toSafeReturnUrl(new URLSearchParams(search).get('returnUrl'));
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

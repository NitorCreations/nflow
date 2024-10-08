import React from 'react';
import {Link as RouterLink} from 'react-router-dom';
import UILink from '@mui/material/Link';

function InternalLink(props: any) {
  return <UILink {...props} component={RouterLink} />;
}

export {InternalLink};

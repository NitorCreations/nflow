import React from 'react';
import {render, screen} from '@testing-library/react';
import App from './App';

test('renders navi', () => {
  render(<App />);
  const logoElement = screen.getByAltText(/nFlow-logo/i);
  expect(logoElement).toBeInTheDocument();

  // these will fail if 0 or more than 1 elements are found
  screen.getByText(/Workflow instances/i);
  screen.getByText(/Executors/i);
  screen.getByText(/About/i);
});

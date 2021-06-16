import React, {useEffect} from 'react';
import * as d3 from 'd3';
import dagreD3 from 'dagre-d3';

import './StateGraph.scss';

// TODO this file is Javascript, typing is missing for dagre-d3

// TODO missing features
// - for instances
//   - highlight current state
//   - current path
//   - retries
//   - mark unsupported paths as red
// - selecting node
//   - highlight incoming/outgoing arrows, bold text
//   - clicking should highlight state in other components e.g. action history
// - selecting state elsewhere should select the state in the graph
// - export graph as a PNG / SVG
// - zoom and pan state graph

// TODO move to service.ts?
function createGraph(definition) {
  const g = new dagreD3.graphlib.Graph().setGraph({});
  const states = definition.states;
  // create nodes
  for (let state of states) {
    g.setNode(state.id, {label: state.id, class: `node-${state.type}`});
    // Round the corners of the nodes
    const node = g.node(state.id);
    node.rx = node.ry = 5;
  }
  // create edges between nodes
  const edgeProps = {
    class: 'edge-normal',
    curve: d3.curveBasis,
    arrowhead: 'normal'
  };

  for (let state of states) {
    for (let transition of state.transitions || []) {
      g.setEdge(state.id, transition, edgeProps);
    }
    if (state.onFailure) {
      g.setEdge(state.id, state.onFailure, {
        ...edgeProps,
        class: 'edge-failure'
      });
    } else if (definition.onError) {
      g.setEdge(state.id, definition.onError, {
        ...edgeProps,
        class: 'edge-error'
      });
    }
  }

  return g;
}

/**
 * Render graph to SVG
 */
function render(g, selector) {
  const render = new dagreD3.render();

  // Set up an SVG group so that we can translate the final graph.
  const svg = d3.select(selector),
    svgGroup = svg.append('g');

  // Run the renderer. This is what draws the final graph.
  render(svgGroup, g);

  // Zoom SVG image to fit the available container
  const svgElem = document.querySelector(selector);
  const bbox = svgElem.getBBox();
  const margin = 5;
  const viewBox = `${bbox.x - margin} ${bbox.y - margin} ${
    bbox.width + 2 * margin
  } ${bbox.height + 2 * margin}`;
  svg.attr('viewBox', viewBox);
}

function StateGraph(props) {
  useEffect(() => {
    console.info('StateGraph', props.definition);
    const g = createGraph(props.definition);
    render(g, 'svg#stategraph');
    return () => {
      // Remove svg element which is created in render(), since it is not managed by React
      const svgContent = d3.select('svg#stategraph g');
      svgContent.remove();
    };
  }, [props.definition]);

  return (
    <div className="svg-container">
      <svg
        id="stategraph"
        className="svg-content-responsive"
        preserveAspectRatio="xMinYMin meet"
      />
    </div>
  );
}

export {StateGraph};

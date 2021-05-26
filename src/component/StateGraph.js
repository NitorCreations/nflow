import React, { useEffect } from "react";
import './StateGraph.css';
// import './graph.css';
import * as d3 from 'd3';

import dagreD3 from 'dagre-d3';

// TODO this file is Javascript, typing is missing for dagre-d3

// TODO move to service.ts?
function createGraph(definition) {
    let g = new dagreD3.graphlib.Graph().setGraph({});

    const states = definition.states
    // create nodes
    for (let state of states) {
        g.setNode(state.id,  { label: state.id, class: "node-" + state.type });
        // Round the corners of the nodes
        let node = g.node(state.id);
        node.rx = node.ry = 5;
    }
    
    // create edges between nodes
    for (let state of states) {
        for (let transition of state.transitions || []) {
            g.setEdge(state.id, transition, {class: "edge-normal", curve: d3.curveBasis, arrowhead: 'normal'});
        }
        if (state.onFailure) {
            g.setEdge(state.id, state.onFailure, {class: "edge-failure", curve: d3.curveBasis, arrowhead: 'normal'})
        }
        if (definition.onError) {
            g.setEdge(state.id, definition.onError, {class: 'edge-error', curve: d3.curveBasis, arrowhead: 'normal'})
        }
    }
    return g;
}

function render(g, selector) {
    // Create the renderer
    var render = new dagreD3.render();

    // Set up an SVG group so that we can translate the final graph.
    var svg = d3.select(selector),
    svgGroup = svg.append("g");

    // Run the renderer. This is what draws the final graph.
    render(d3.select("svg"), g);

    // Center the graph
    var xCenterOffset = (svg.attr("width") - g.graph().width) / 2;
    //svgGroup.attr("transform", "translate(" + xCenterOffset + ", 20)");
    svg.attr("height", g.graph().height + 40);
    svg.attr("width", g.graph().height + 140);
}

function StateGraph(props) {
    console.info('StateGraph', props.definition);
    const g = createGraph(props.definition);
    useEffect(() => {
        render(g, '#stategraph svg')
    })
    return <div id="stategraph">Jee stategraph<svg/></div>;
}

export { StateGraph };

'use strict';

function nodeDomId(nodeId) {
  return 'node_' + nodeId;
}
function edgeDomId(edgeId) {
  return 'edge' + edgeId;
}

function nodeColors(nodeId, colors) {
  var rect = $('#' + nodeDomId(nodeId) + ' rect');
  rect.attr('fill', colors.background);
  rect.css('stroke', colors.border);

  var text = $('#' + nodeDomId(nodeId) + ' g text');
  text.attr('fill', colors.text);
}

function disableZoomPan() {
  var svg =  d3.select('svg');

  // panning off
  svg.on('mousedown.zoom', null);
  svg.on('mousemove.zoom', null);
  // zooming off
  svg.on('dblclick.zoom', null);
  svg.on('touchstart.zoom', null);
  svg.on('wheel.zoom', null);
  svg.on('mousewheel.zoom', null);
  svg.on('MozMousePixelScroll.zoom', null);
}

function nodeEdges(graph, nodeId) {
  var inEdges = _.flatten(_.map(graph._inEdges[nodeId], function(e) {
    return e.keys();
  }));
  var outEdges = _.flatten(_.map(graph._outEdges[nodeId], function(e) {
    return e.keys();
  }));
  return _.flatten([inEdges, outEdges]);
}

function highlightEdges(graph, nodeId) {
  _.each(nodeEdges(graph, nodeId), function(edgeId) {
    $('#' + edgeDomId(edgeId)).css('stroke-width', '2px');
  });
}

function unhighlightEdges(graph, nodeId) {
  _.each(nodeEdges(graph, nodeId), function(edgeId) {
    $('#' + edgeDomId(edgeId)).css('stroke-width', '1px');
  });
}

function nodeRect(nodeId) {
  return $('#' + nodeDomId(nodeId) + ' rect');
}

function higlightNode(graph, workflow, nodeId) {
  highlightEdges(graph, nodeId);
  nodeRect(nodeId).css('stroke-width', '3px');
  var state = _.find(workflow.states,
                     function(state) {
                       return state.id === nodeId;
                     });
  state.selected = 'highlight';
}

function unhiglightNode(graph, workflow, nodeId) {
  unhighlightEdges(graph, nodeId);
  nodeRect(nodeId).css('stroke-width', '1.5px');
  _.each(workflow.states, function(state) {
    state.selected = undefined;
  });
}

/**
 * Count how many times this state has been retried. Including non-consecutive retries.
 */
function calculateRetries(workflow, state) {
  var retries = 0;
  if(workflow) {
    _.each(workflow.actions, function(action) {
      if(action.state === state.id && action.retryNo > 0)  {
        retries ++;
      }});
  }
  return retries;
}

function activeNode(workflow, state) {
  if(!workflow) {
    return true;
  }
  if(workflow.state === state.name) {
    return true;
  }
  return !!_.find(workflow.actions, function(action) {
    return action.state === state.name;
  });
}

function createNodeStyle(state, workflow, unexpected) {
  var active = activeNode(workflow, state);
  var labelStroke = '';
  var boxStroke = 'black';
  if(!active) {
    boxStroke = 'gray';
    labelStroke = 'fill: gray;';
  }
  if(unexpected) {
    boxStroke = 'red';
    labelStroke = 'fill: red;';
  }
  var normalNodeStyle = {labelStyle: 'font-size: 14px; ' + labelStroke +
                         'font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;',
                         stroke: boxStroke,
                         style: 'stroke-width: 1.5px;',
                        };

  var startNodeStyle = {labelStyle: 'font-size: 14px;' + labelStroke +
                        'font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;',
                        stroke: boxStroke,
                        fill: 'LightBlue',
                        style: 'stroke-width: 1.5px;',
                       };


  var errorNodeStyle = {labelStyle: 'font-size: 14px;' + labelStroke +
                        'font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;',
                        stroke: boxStroke,
                        fill: 'yellow',
                        style: 'stroke-width: 1.5px;',
                       };

  var endNodeStyle = {labelStyle: 'font-size: 14px;' + labelStroke +
                      'font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;',
                      stroke: boxStroke,
                      fill: 'LightGreen',
                      style: 'stroke-width: 1.5px;',
                     };

  var nodeStyle = normalNodeStyle;
  if(state.type === 'start') {
    nodeStyle = startNodeStyle;
  }
  if(state.type === 'manual') {
    nodeStyle = errorNodeStyle;
  }
  if(state.type === 'end') {
    nodeStyle = endNodeStyle;
  }
  if(state.type === 'error') {
    nodeStyle = errorNodeStyle;
  }

  nodeStyle.retries = calculateRetries(workflow, state);
  nodeStyle.label = state.name;
  return nodeStyle;
}

function activeTransition(workflow, state, transition) {
  if(!workflow) {
    return true;
  }
  if(workflow.actions.length < 2) {
    return false;
  }

  var first = null;
  var found =  _.find(workflow.actions, function(action) {
    if(!first) {
      first = action.state;
      return false;
    }
    if(first === state.name && action.state === transition) {
      return true;
    }
    first = action.state;
  });
  if(found) {
    return found;
  }
  return _.last(workflow.actions).state === state.name && workflow.state === transition;
}

function createEdgeStyle(workflow, definition, state, transition, genericError) {
  // TODO when active, line should be thicker, but note also higlightNode()
  if(!workflow || activeTransition(workflow, state, transition)) {
    if(genericError) {
      return {style: 'stroke: black; fill: none; stroke-dasharray: 5,5'};
    }
    return {style: 'stroke: black; fill: none;'};
  } else {
    if(genericError) {
      return {style: 'stroke: gray; fill: none; stroke-dasharray: 5,5'};
    }
    return {style: 'stroke: gray; fill: none;'};
  }
}

function addUnexpectedNodes(g, workflow) {
  if(!workflow) {
    return;
  }
  _.each(workflow.actions, function(action) {
    if(g._nodes[action.state]) {
      return;
    }
    var nodeStyle = createNodeStyle({name: action.state}, workflow, true);
    g.addNode(action.state, nodeStyle);
  });
}

function addUnexpectedEdges(g, workflow) {
  if(!workflow) {
    return;
  }
  var activeEdges = {};
  var sourceState = null;
  _.each(workflow.actions, function(action) {
    if(!activeEdges[action.state]) {
      activeEdges[action.state] = {};
    }
    if(!sourceState) {
      sourceState = action.state;
      return;
    }

    // do not include retries
    if(sourceState !== action.state) {
      activeEdges[sourceState][action.state] = true;
    }
    sourceState = action.state;
  });

  // handle last action -> currentAction, do not include retries
  var lastAction = _.last(workflow.actions);
  if(lastAction && lastAction.state !== workflow.state) {
    activeEdges[lastAction.state][workflow.state] = true;
  }

  _.each(activeEdges, function(targetObj, source) {
    var target = Object.keys(targetObj)[0];
    if(!target) { return; }
    if(!g.inEdges(target, source).length) {
      g.addEdge(null, source, target,
                {style: 'stroke: red; fill: none;'});
    }
  });
}

function workflowDefinitionGraph(definition, workflow) {
  var g = new dagreD3.Digraph();
  // All nodes must be added to graph before edges

  // Exported SVG has silly defaults for some styles
  // so they are overridden here.
  for(var i in definition.states) {
    var state = definition.states[i];

    var nodeStyle = createNodeStyle(state, workflow);
    g.addNode(state.name, nodeStyle);
  }
  // Add nodes not in workflow definition
  addUnexpectedNodes(g, workflow);

  // Add edges
  for(var edgeIndex in definition.states) {
    var state = definition.states[edgeIndex];
    for(var k in state.transitions){
      var transition = state.transitions[k];
      g.addEdge(null, state.name, transition,
                createEdgeStyle(workflow, definition, state, transition));
    }
    if(state.onFailure) {
      g.addEdge(null, state.name, state.onFailure,
                createEdgeStyle(workflow, definition, state, state.onFailure, true));

    }
  }

  // Add edges to generic onError state
  var errorStateName = definition.onError;
  _.each(definition.states, function(state) {
    if(state.name === errorStateName || state.onFailure || state.type === 'end') {
      return;
    }
    if(_.contains(state.transitions, errorStateName)) {
      return;
    }
    g.addEdge(null, state.name, errorStateName,
             createEdgeStyle(workflow, definition, state, errorStateName, true));
  });

  // add edges that are not present in workflow definition
  addUnexpectedEdges(g, workflow);
  return g;
}

function drawWorkflowDefinition(graph, canvasId, nodeSelectedCallBack) {
  var renderer = new dagreD3.Renderer();
  var oldDrawNodes = renderer.drawNodes();
  renderer.drawNodes(
    function(g, root) {
      var nodes = oldDrawNodes(graph, root);

      // use hand mouse cursor for nodes
      nodes.attr('style',
                 function(e) {
                   return 'opacity: 1;cursor: pointer;';
                 });
      // add id attr to nodes g elements
      nodes.attr('id', function(nodeId) {
                                  return nodeDomId(nodeId);
                                });

      // draw retry indicator
      // fetch sizes for node rects => needed for calculating right edge for rect
      var nodeCoords = {};
      nodes.selectAll('rect').each(function (nodeName) {
        var t = d3.select(this);
        nodeCoords[nodeName] = {x: t.attr('x'), y: t.attr('y')};
      });

      // orange ellipse with retry count
      var retryGroup = nodes.append('g');
      retryGroup.each(function(nodeId) {
        var node = g.node(nodeId);
        if(node.retries > 0) {
          var c = nodeCoords[nodeId];
          var t = d3.select(this);
          t.attr('transform', 'translate(' + (- c.x) + ',-4)');

          t.append('ellipse')
          .attr('cx', 10).attr('cy', -5)
          .attr('rx', 20).attr('ry', 10)
          .attr('style', 'fill: orange; stroke: black; stroke-width: 1.5px');

          t.append('text')
          .append('tspan')
          .text(node.retries);

          t.append('title').text('State was retried ' + node.retries + ' times.');
        }
      });
      // event handler for clicking nodes
      nodes.on('click', function(nodeId) {
        nodeSelectedCallBack(nodeId);
      });
      return nodes;
    });

  var oldDrawEdgePaths = renderer.drawEdgePaths();
  renderer.drawEdgePaths(
    function(g, root) {
      var edges = oldDrawEdgePaths(g, root);
      // add id to edges
      edges.selectAll('*').attr('id', function(edgeId) {
        return edgeDomId(edgeId);
      });
      return edges;
    });

  var svgRoot = d3.select('#' + canvasId), svgGroup = svgRoot.append('g');

  var layout = renderer.run(graph, svgGroup);
  var svgBackground = svgRoot.select('rect.overlay');
  svgBackground.attr('style', 'fill: white; pointer-events: all;');
  svgBackground.on('click', function() {
    // event handler for clicking outside nodes
    nodeSelectedCallBack(null);
  });


  svgGroup.attr('transform', 'translate(20, 20)');
  svgRoot.attr('height', layout.graph().height + 40);
  svgRoot.attr('width', layout.graph().width + 40);
  disableZoomPan();
  return layout;
}

function downloadDataUrl(dataurl, filename) {
  var a = document.createElement('a');
  // http://stackoverflow.com/questions/12112844/how-to-detect-support-for-the-html5-download-attribute
  // TODO firefox supports download attr, but due security doesn't work in our case
  if('download' in a) {
    console.debug('Download via a.href,a.download');
    a.download = filename;
    a.href = dataurl;
    a.click();
  } else {
    console.debug('Download via location.href');
    // http://stackoverflow.com/questions/12676649/javascript-programmatically-trigger-file-download-in-firefox
    location.href = dataurl;
  }
}


function downloadImage(dataurl, filename, contentType) {
  console.info('Downloading image', filename, contentType);
  var canvas = document.createElement('canvas');

  var context = canvas.getContext('2d');
  var svg = $('svg');
  canvas.height = svg.attr('height');
  canvas.width = svg.attr('width');
  var image = new Image();
  image.onload = function() {
    // image load is async, must use callback
    context.drawImage(image, 0, 0);
    var canvasdata = canvas.toDataURL(contentType);
    downloadDataUrl(canvasdata, filename);
  };
  image.onerror = function(error) {
    console.error('Image downloading failed', error);
  };
  image.src = dataurl;
}

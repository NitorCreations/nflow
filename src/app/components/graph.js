(function () {
  'use strict';

  var m = angular.module('nflowExplorer.graph', []);

  m.factory('Graph', function() {
    return {
      setNodeSelected: setNodeSelected,
      markCurrentState: markCurrentState,
      workflowDefinitionGraph: workflowDefinitionGraph,
      drawWorkflowDefinition: drawWorkflowDefinition,
      downloadDataUrl: downloadDataUrl,
      downloadImage: downloadImage
    };

    function setNodeSelected(graph, nodeId, isTrue) {
      _.each(graph.predecessors(nodeId), function(prev) { setEdgeSelected(prev, nodeId); });
      _.each(graph.successors(nodeId), function(next) { setEdgeSelected(nodeId, next); });
      setSelected('#' + nodeDomId(nodeId));

      function setEdgeSelected(source, target) {
        _.each(graph.incidentEdges(source, target), function(edgeId) { setSelected('#' + edgeDomId(edgeId)); });
      }

      function setSelected(selector) { d3.select(selector).classed('selected', isTrue); }
    }
  });

// TODO remove jshint exception
// jshint unused:false


function nodeDomId(nodeId) {
  return 'node_' + nodeId;
}
function edgeDomId(edgeId) {
  return 'edge' + edgeId;
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

function markCurrentState(workflow) {
  d3.select('#' + nodeDomId(workflow.state)).classed('current-state', true);
}

function workflowDefinitionGraph(definition, workflow) {
  var g = new dagreD3.Digraph();
  // NOTE: all nodes must be added to graph before edges
  addNodes();
  addEdges();
  return g;

  function addNodes() {
    addNodesThatArePresentInWorkflowDefinition();
    addNodesThatAreNotPresentInWorkflowDefinition();
    return;

    function addNodesThatArePresentInWorkflowDefinition() {
      for(var i in definition.states) {
        var stateNode = definition.states[i];

        var nodeStyle = createNodeStyle(stateNode, workflow);
        g.addNode(stateNode.name, nodeStyle);
      }
    }

    function addNodesThatAreNotPresentInWorkflowDefinition() {
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

    function createNodeStyle(state, workflow, unexpected) {
      var active = activeNode(workflow, state);
      var labelStroke = '';
      var boxStroke = 'black';
      var strokeWidth = '3px';
      if(!active) {
        boxStroke = 'gray';
        labelStroke = 'fill: gray;';
        strokeWidth = '1.5px';
      }
      if(!workflow) {
        strokeWidth = '1.5px';
      }
      if(unexpected) {
        boxStroke = 'red';
        labelStroke = 'fill: red;';
      }

      var nodeStyle = {'class': 'node-normal'};
      if(state.type === 'start') {
        nodeStyle = {'class': 'node-start'};
      }
      if(state.type === 'manual') {
        nodeStyle = {'class': 'node-manual'};
      }
      if(state.type === 'end') {
        nodeStyle = {'class': 'node-end'};
      }
      if(state.type === 'error') {
        nodeStyle = {'class': 'node-error'};
      }
      if(workflow && !active) {
        nodeStyle['class'] += ' node-passive';
      }

      nodeStyle.retries = calculateRetries(workflow, state);
      nodeStyle.state = state;
      nodeStyle.label = state.name;
      return nodeStyle;
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
  }

  function addEdges() {
    addEdgesThatArePresentInWorkflowDefinition();
    addEdgesToGenericOnErrorState();
    addEdgesThatAreNotPresentInWorkflowDefinition();
    return;

    function addEdgesThatArePresentInWorkflowDefinition() {
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
    }

    function addEdgesToGenericOnErrorState() {
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
    }

    function addEdgesThatAreNotPresentInWorkflowDefinition() {
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
        _.each(Object.keys(targetObj), function(target) {
          if(!target) { return; }
          if(!g.inEdges(target, source).length) {
            g.addEdge(null, source, target,
              {'class': 'edge-unexpected edge-active'});
          }
        });
      });
    }

    function createEdgeStyle(workflow, definition, state, transition, genericError) {
      if(!workflow) {
        if(genericError) {
          return {'class': 'edge-error'};
        }
        return {'class': 'edge-normal'};
      }
      if(activeTransition(workflow, state, transition)) {
        if(genericError) {
          return {'class': 'edge-error edge-active'};
        }
        return {'class': 'edge-normal edge-active'};
      } else {
        if(genericError) {
          return {'class': 'edge-error edge-passive'};
        }
        return {'class': 'edge-normal edge-passive'};
      }
    }
  }
}

function drawWorkflowDefinition(graph, canvasSelector, nodeSelectedCallBack, embedCSS) {
  var renderer = new dagreD3.Renderer();

  drawNodes(renderer, graph, nodeSelectedCallBack);
  drawEdges(renderer);

  var svgRoot = initSvg(canvasSelector, embedCSS);
  var layout = initLayout(renderer, graph, svgRoot);
  drawArrows(canvasSelector);

  configureSvg(nodeSelectedCallBack, svgRoot, layout);

  return layout;

  function initSvg(canvasSelector, embedCSS) {
    var svgRoot = d3.select(canvasSelector);
    svgRoot.selectAll('*').remove();
    svgRoot.append('style').attr('type', 'text/css').text(embedCSS);
    svgRoot.classed('svg-content-responsive', true);
    return svgRoot;
  }

  function configureSvg(nodeSelectedCallBack, svgRoot, layout) {
    configureOverlay();
    disableZoomPan();
    configureSize();

    function configureOverlay() {
      var svgBackground = svgRoot.select('rect.overlay');
      svgBackground.attr('style', '');
      svgBackground.attr('class', 'graph-background');
      svgBackground.on('click', function() {
        // event handler for clicking outside nodes
        nodeSelectedCallBack(null);
      });
    }

    function disableZoomPan() {
      // panning off
      svgRoot.on('mousedown.zoom', null);
      svgRoot.on('mousemove.zoom', null);
      // zooming off
      svgRoot.on('dblclick.zoom', null);
      svgRoot.on('touchstart.zoom', null);
      svgRoot.on('wheel.zoom', null);
      svgRoot.on('mousewheel.zoom', null);
      svgRoot.on('MozMousePixelScroll.zoom', null);
    }

    function configureSize() {
      svgRoot.attr('preserveAspectRatio', 'xMinYMin meet');
      svgRoot.attr('viewBox', '0 0 ' + (layout.graph().width+40) + ' ' + (layout.graph().height+40));
    }
  }

  function initLayout(renderer, graph, svgRoot) {
    var svgGroup = svgRoot.append('g');
    svgGroup.attr('transform', 'translate(20, 20)');
    return  renderer.run(graph, svgGroup);
  }

  function drawNodes(renderer, graph, nodeSelectedCallBack) {
    var oldDrawNodes = renderer.drawNodes();
    renderer.drawNodes(
      function(g, root) {
        var nodes = oldDrawNodes(graph, root);

        // use hand mouse cursor for nodes
        nodes.attr('style',
          function() {
            return 'opacity: 1;cursor: pointer;';
          });
        nodes.append('title').text(function(nodeId){
          var node = g.node(nodeId);
          return  capitalize(node.state.type) + ' state\n' +
            node.state.description;
        });
        // add id attr to nodes g elements
        nodes.attr('id', function(nodeId) {
          return nodeDomId(nodeId);
        });
        nodes.attr('class', function(nodeId) {
          // see createEdgeStyle, class is not supported attribute
          return g.node(nodeId)['class'];
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
              .attr('class', 'retry-indicator');

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
  }

  function drawEdges(renderer) {
    var oldDrawEdgePaths = renderer.drawEdgePaths();
    renderer.drawEdgePaths(
      function(g, root) {
        var edges = oldDrawEdgePaths(g, root);
        // add id to edges
        edges.selectAll('*').attr('id', function(edgeId) {
          return edgeDomId(edgeId);
        })
          .attr('class', function(edgeId) {
            // see createEdgeStyle, class is not supported attribute
            return g._edges[edgeId].value.class;
          });
        return edges;
      });
  }

  function drawArrows(canvasSelector) {
    addArrowheadMarker(canvasSelector, 'arrowhead-gray', 'gray');
    addArrowheadMarker(canvasSelector, 'arrowhead-red', 'red');
  }

  function addArrowheadMarker(canvasSelector, id, color) {
    d3.select(canvasSelector).select('defs')
      .append('marker')
      .attr('id', id)
      .attr('viewBox', '0 0 10 10')
      .attr('refX', 8)
      .attr('refY', '5')
      .attr('markerUnits', 'strokeWidth')
      .attr('markerWidth', '8')
      .attr('markerHeight', '5')
      .attr('orient', 'auto')
      .attr('fill', color)
      .append('path')
      .attr('d', 'M 0 0 L 10 5 L 0 10 z');
  }
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

function downloadImage(size, dataurl, filename, contentType) {
  console.info('Downloading image', filename, contentType);
  var canvas = document.createElement('canvas');

  var context = canvas.getContext('2d');
  canvas.width = size[0];
  canvas.height = size[1];
  var image = new Image();
  image.width = canvas.width;
  image.height = canvas.height;
  image.onload = function() {
    // image load is async, must use callback
    context.drawImage(image, 0, 0, this.width, this.height);
    var canvasdata = canvas.toDataURL(contentType);
    downloadDataUrl(canvasdata, filename);
  };
  image.onerror = function(error) {
    console.error('Image downloading failed', error);
  };
  image.src = dataurl;
}
})();

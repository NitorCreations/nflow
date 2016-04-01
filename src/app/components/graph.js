(function () {
  'use strict';

  var m = angular.module('nflowExplorer.components.graph', []);

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

    function markCurrentState(workflow) {
      d3.select('#' + nodeDomId(workflow.state)).classed('current-state', true);
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
          _.forEach(definition.states, function(state) { g.addNode(state.id, createNodeStyle(state, workflow)); });
        }

        function addNodesThatAreNotPresentInWorkflowDefinition() {
          _.chain(_.result(workflow, 'actions'))
            .filter(function(action) { return !g._nodes(action.state); })
            .forEach(function(action) { g.addNode(action.state, createNodeStyle({name: action.state}, workflow)); })
          ;
        }

        function createNodeStyle(state, workflow) {
          var nodeStyle = {};
          nodeStyle['class'] = resolveStyleClass();
          nodeStyle.retries = calculateRetries();
          nodeStyle.state = state;
          nodeStyle.label = state.id;
          return nodeStyle;

          function resolveStyleClass() {
            var cssClass = 'node-' + (_.includes(['start', 'manual', 'end', 'error'], state.type) ? state.type : 'normal');
            if(workflow && !isActiveNode()) { cssClass += ' node-passive'; }
            return cssClass;

            function isActiveNode() {
              return workflow.state === state.id || !_.isUndefined(_.find(workflow.actions, 'state', state.id));
            }
          }

          /**
           * Count how many times this state has been retried. Including non-consecutive retries.
           */
          function calculateRetries() {
            return _.reduce(_.result(workflow, 'actions'), function(acc, action) {
              return action.state === state.id && action.retryNo > 0 ? acc+1 : acc;
            }, 0);
          }
        }
      }

      function addEdges() {
        addEdgesThatArePresentInWorkflowDefinition();
        addEdgesToGenericOnErrorState();
        addEdgesThatAreNotPresentInWorkflowDefinition();
        return;

        function addEdgesThatArePresentInWorkflowDefinition() {
          _.forEach(definition.states, function(state) {
            _.forEach(state.transitions, function(transition) {
              g.addEdge(null, state.id, transition, createEdgeStyle(workflow, state, transition));
            });

            if (state.onFailure) {
              g.addEdge(null, state.id, state.onFailure, createEdgeStyle(workflow, state, state.onFailure, true));
            }
          });
        }

        function addEdgesToGenericOnErrorState() {
          var errorStateId = definition.onError;
          _.forEach(definition.states, function(state) {
            if (state.id !== errorStateId && !state.onFailure && state.type !== 'end' &&
              !_.contains(state.transitions, errorStateId)) {
              g.addEdge(null, state.id, errorStateId, createEdgeStyle(workflow, state, errorStateId, true));
            }
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

        function createEdgeStyle(workflow, state, transition, genericError) {
          return { 'class': resolveStyleClass() };

          function resolveStyleClass() {
            var cssStyle = 'edge-' + (genericError ? 'error' : 'normal');
            if (workflow) {
              cssStyle += ' edge-' + (isActiveTransition(state, transition) ? 'active' : 'passive');
            }
            return cssStyle;

            function isActiveTransition(state, transition) {
              if(_.size(workflow.actions) < 2) { return false; }

              var prevState = _.first(workflow.actions).state;
              var found =  _.find(_.rest(workflow.actions), function(action) {
                if (prevState === state.id && action.state === transition) { return true; }
                prevState = action.state;
              });

              return !_.isUndefined(found) || _.last(workflow.actions).state === state.id && workflow.state === transition;
            }
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
        renderer.drawNodes(function(g, root) {
          var nodes = oldDrawNodes(graph, root);
          nodes.attr('style', function() { return 'opacity: 1; cursor: pointer;'; });
          nodes.append('title').text(function(nodeId){ return buildTitle(g.node(nodeId).state); });
          nodes.attr('id', function(nodeId) { return nodeDomId(nodeId); });
          nodes.attr('class', function(nodeId) { return g.node(nodeId)['class']; });
          nodes.on('click', function(nodeId) { nodeSelectedCallBack(nodeId); });
          drawRetryIndicator();
          return nodes;

          function drawRetryIndicator() {
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
              if (node.retries > 0) {
                var c = nodeCoords[nodeId];
                var t = d3.select(this);
                t.attr('transform', 'translate(' + (- c.x) + ',-4)');

                t.append('ellipse')
                  .attr('cx', 10).attr('cy', -5)
                  .attr('rx', 20).attr('ry', 10)
                  .attr('class', 'retry-indicator');

                t.append('text').append('tspan').text(node.retries);
                t.append('title').text('State was retried ' + node.retries + ' times.');
              }
            });
          }

          function buildTitle(state) { return _.capitalize(state.type) + ' state\n' + state.description; }
        });
      }

      function drawEdges(renderer) {
        var oldDrawEdgePaths = renderer.drawEdgePaths();
        renderer.drawEdgePaths(function(g, root) {
          var edges = oldDrawEdgePaths(g, root);
          edges.selectAll('*')
            .attr('id', function(edgeId) { return edgeDomId(edgeId); })
            .attr('class', function(edgeId) { return g._edges[edgeId].value.class; });
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

    function nodeDomId(nodeId) { return 'node_' + nodeId; }

    function edgeDomId(edgeId) { return 'edge' + edgeId; }
  });
})();

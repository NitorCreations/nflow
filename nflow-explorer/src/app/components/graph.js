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
      _.each(graph.nodeEdges(nodeId), function(edge) {
        setSelected('#' + edgeDomId(edge.v, edge.w));
      });
      setSelected('#' + nodeDomId(nodeId));

      function setSelected(selector) {
        d3.select(selector).classed('selected', isTrue);
      }
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
      var g = new dagreD3.graphlib.Graph().setGraph({});
      // NOTE: all nodes must be added to graph before edges
      addNodes();
      addEdges();
      return g;

      function addNodes() {
        addNodesThatArePresentInWorkflowDefinition();
        addNodesThatAreNotPresentInWorkflowDefinition();

        function addNodesThatArePresentInWorkflowDefinition() {
          _.forEach(definition.states, function(state) {
              g.setNode(state.id, createNodeAttributes(state, workflow));
          });
        }

        function addNodesThatAreNotPresentInWorkflowDefinition() {
          //workflow && workflow.actions.push({id: 999999, state: 'dummy'}); // for testing
          (_.result(workflow, 'actions') || [])
            .filter(function(action) {
              return !g.hasNode(action.state);
            })
            .forEach(function(action) {
              g.setNode(action.state, createNodeAttributes({id: action.state}, workflow));
            });
        }

        function createNodeAttributes(state, workflow) {
          return {
            rx: 5,
            ry: 5,
            class: resolveStyleClass(),
            retries: calculateRetries(),
            state: state,
            label: state.id,
            id: nodeDomId(state.id),
            shape: 'rect'
          };

          function resolveStyleClass() {
            var cssClass = 'node-' + (_.includes(['start', 'manual', 'end', 'error'], state.type) ? state.type : 'normal');
            if (workflow && isPassiveNode()) {
              cssClass += ' node-passive';
            }
            return cssClass;

            function isPassiveNode() {
              return workflow.state !== state.id && _.isUndefined(_.find(workflow.actions, function(action) {
                return action.state === state.id;
              }));
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
        if (workflow) {
          addEdgesThatAreNotPresentInWorkflowDefinition();
        }

        function addEdgesThatArePresentInWorkflowDefinition() {
          _.forEach(definition.states, function(state) {
            _.forEach(state.transitions, function(transition) {
              setEdge(state.id, transition, 'normal');
            });
            var failureState = state.onFailure || definition.onError;
            if (state.type !== 'end' && failureState !== state.id) {
              setEdge(state.id, failureState, 'error');
            }
          });
        }

        function addEdgesThatAreNotPresentInWorkflowDefinition() {
          var sourceState = null;
          var actions = workflow.actions.slice().reverse();
          actions.push({state: workflow.state});
          _.each(actions, function(action) {
            if (sourceState && sourceState !== action.state) {
              if (!g.hasEdge(sourceState, action.state)) {
                setEdge(sourceState, action.state, 'unexpected');
              } else {
                var edgeAttributes = g.edge(sourceState, action.state);
                edgeAttributes.class = edgeAttributes.class + ' active';
              }
            }
            sourceState = action.state;
          });
        }

        function setEdge(state, transition, style) {
          g.setEdge(state, transition, {
            id: edgeDomId(state, transition),
            class: 'edge-' + style,
            arrowheadClass: 'arrowhead-' + style,
            curve: d3.curveBasis
          });
        }
      }
    }

    function drawWorkflowDefinition(graph, canvasSelector, nodeSelectedCallBack, embedCSS) {
      var svgRoot = initSvg(canvasSelector, embedCSS);
      svgRoot.attr('preserveAspectRatio', 'xMinYMin meet');
      var svgGroup = svgRoot.append('g');
      var render = new dagreD3.render();
      render(svgGroup, graph);
      decorateNodes(canvasSelector, graph, nodeSelectedCallBack);
      setupAndApplyZoom(graph, svgRoot, svgGroup);

      function initSvg(canvasSelector, embedCSS) {
        var svgRoot = d3.select(canvasSelector);
        svgRoot.selectAll('*').remove();
        svgRoot.append('style').attr('type', 'text/css').text(embedCSS);
        svgRoot.classed('svg-content-responsive', true);
        return svgRoot;
      }

      function decorateNodes(canvasSelector, g, nodeSelectedCallBack) {
        // note: always operate on the first canvas, as there can be two present in DOM
        // simultaneously during UI state transitions
        var nodes = d3.select(canvasSelector).selectAll('.nodes > g');
        nodes.append('title').text(function(nodeId){ return buildTitle(g.node(nodeId).state); });
        nodes.attr('id', function(nodeId) { return nodeDomId(nodeId); });
        nodes.attr('class', function(nodeId) { return g.node(nodeId)['class']; });
        nodes.on('click', function(nodeId) { nodeSelectedCallBack(nodeId); });
        drawRetryIndicator();

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

        function buildTitle(state) {
          return _.capitalize(state.type) + ' state\n' + state.description;
        }
      }

      function setupAndApplyZoom(graph, svgRoot, svgGroup) {
        var zoom = d3.zoom().on('zoom', function() {
          svgGroup.attr('transform', d3.event.transform);
        });
        svgRoot.call(zoom);
        var aspectRatio = graph.graph().height / graph.graph().width;
        var availableWidth = parseInt(svgRoot.style('width').replace(/px/, ''));
        svgRoot.attr('height', Math.max(Math.min(availableWidth * aspectRatio, graph.graph().width * aspectRatio) + 60, 300));
        var zoomScale = Math.min(availableWidth / (graph.graph().width + 70), 1);
        svgRoot.call(zoom.transform, d3.zoomIdentity.scale(zoomScale).translate(35, 30));
      }

    }

    function nodeDomId(nodeId) {
      return 'node_' + nodeId;
    }

    function edgeDomId(srcNodeId, trgNodeId) {
      return 'edge-' + srcNodeId + '-' + trgNodeId;
    }
  });
})();

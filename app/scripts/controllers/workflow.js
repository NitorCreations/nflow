'use strict';
/**
 * Display single workflow definition
 */
var app = angular.module('nflowVisApp');

app.factory('WorkflowDefinitions', function ($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-definition', {type: '@type'},
                   {'get': {isArray: true}});
});


app.factory('Workflows', function ($resource) {
  return $resource(config.nflowUrl + '/v1/workflow-instance/:id', {id: '@id', include: 'actions'}, {'update': {method: 'PUT'}});
});

app.controller('WorkflowCtrl', function ($scope, WorkflowDefinitions, $routeParams) {

  function nodeDomId(nodeId) {
    return 'node_' + nodeId;
  }
  function edgeDomId(edgeId) {
    return 'edge' + edgeId;
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

  function nodeEdges(nodeId) {
    var g = $scope.graph;
    var inEdges = _.flatten(_.map(g._inEdges[nodeId], function(e) {
      return e.keys();
    }));
    var outEdges = _.flatten(_.map(g._outEdges[nodeId], function(e) {
      return e.keys();
    }));
    return _.flatten([inEdges, outEdges]);
  }

  function highlightEdges(nodeId) {
    _.each(nodeEdges(nodeId), function(edgeId) {
      $('#' + edgeDomId(edgeId)).css('stroke-width', '2px');
    });
  }

  function unhighlightEdges(nodeId) {
    _.each(nodeEdges(nodeId), function(edgeId) {
      $('#' + edgeDomId(edgeId)).css('stroke-width', '1px');
    });
  }

  function higlightNode(nodeId) {
    highlightEdges(nodeId);
    $('#' + nodeDomId(nodeId)).css('stroke-width', '3px');
    var state = _.find($scope.workflow.states,
                       function(state) {
                         return state.id === nodeId;
                       });
    state.selected = 'highlight';
  }

  function unhiglightNode(nodeId) {
    unhighlightEdges(nodeId);
    $('#' + nodeDomId(nodeId)).css('stroke-width', '1.5px');
    _.each($scope.workflow.states, function(state) {
      state.selected = undefined;
    });
  }

  /** called when node is clicked */
  function nodeSelected(nodeId) {
    console.debug('Selecting node ' + nodeId);
    if($scope.selectedNode) {
      unhiglightNode($scope.selectedNode);
    }
    if(nodeId) {
      higlightNode(nodeId);
    }
    $scope.selectedNode = nodeId;
  }

  $scope.nodeSelected = nodeSelected;

  function drawWorkflowDefinition(definition, canvasId) {
    var g = new dagreD3.Digraph();
    // All nodes must be in graph before edges

    // Exported SVG has silly defaults for some styles
    // so they are overridden here.
    for(var i in definition.states) {
      var state = definition.states[i];
      g.addNode(state.name, {label: state.name,
                             labelStyle: 'font-size: 14px;' +
                             'font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;',
                             stroke: 'black',
                             style: 'stroke-width: 1.5px;'
                             });
    }

    // Add edges
    for(var edgeIndex in definition.states) {
      var state = definition.states[edgeIndex];
      for(var k in state.transitions){
        var transition = state.transitions[k];
        g.addEdge(null, state.name, transition,
                  {style: 'stroke: black; fill: none;'});
      }
    }

    var renderer = new dagreD3.Renderer();
    var oldDrawNodes = renderer.drawNodes();
    renderer.drawNodes(
      function(g, root) {
        var nodes = oldDrawNodes(g, root);
        // add id attr to nodes' background rects
        nodes.select('rect').attr('id',
                                  function(nodeId) {
                                    return nodeDomId(nodeId);
                                  });
        // use hand mouse cursor for nodes
        nodes.attr('style',
                  function(e) {
                    return "opacity: 1;cursor: pointer;";
                  });
        // event handler for clicking nodes
        nodes.on('click', function(nodeId) {
          // must use $apply() - event not managed by angular
          $scope.$apply(function() {
            nodeSelected(nodeId);
          });
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

    var layout = renderer.run(g, svgGroup);
    var svgBackground = svgRoot.select('rect.overlay');
    svgBackground.attr('style', 'fill: white; pointer-events: all;');
    svgBackground.on('click', function() {
      // event handler for clicking outside nodes
      // must use $apply() - event not managed by angular
      $scope.$apply(function() {
        nodeSelected(null);
      });
    });


    svgGroup.attr('transform', 'translate(20, 20)');
    svgRoot.attr('height', layout.graph().height + 40);
    svgRoot.attr('width', layout.graph().width + 40);
    disableZoomPan();
    return g;
  }

  WorkflowDefinitions.get({type: $routeParams.type},
                          function(data) {
                            var start = new Date().getTime();
                            var definition =  _.first(data);
                            $scope.workflow = definition;
                            $scope.graph = drawWorkflowDefinition(definition, 'dagreSvg');
                            console.debug('Rendering dagre graph took ' +
                                          (new Date().getTime() - start) + ' msec' );
                          });

  function svgDataUrl() {
    var html = d3.select('svg')
      .attr('version', 1.1)
      .attr('xmlns', 'http://www.w3.org/2000/svg')
      .node().outerHTML;
    return 'data:image/svg+xml;base64,'+ btoa(html);
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
    //var canvas = document.getElementById('dagreCanvas');

    var context = canvas.getContext('2d');
    var svg = $('svg');
    canvas.height = svg.attr('height');
    canvas.width = svg.attr('width');
    var image = new Image();
    image.onload = function() {
      // image load is async, must use callback
      context.drawImage(image, 0, 0);
      var canvasdata = canvas.toDataURL(contentType);
      console.log(contentType, canvasdata);
      downloadDataUrl(canvasdata, filename);
    };
    image.onerror = function(error) {
      console.error('Image downloading failed', error);
    };
    image.src = dataurl;
  }

  function downloadSvg(filename) {
    downloadDataUrl(svgDataUrl(), filename);
  }

  $scope.savePng = function savePng() {
    console.log('Save PNG');
    var selectedNode = $scope.selectedNode;
    nodeSelected(null);
    downloadImage(svgDataUrl(), $scope.workflow.type + '.png', 'image/png');
    nodeSelected(selectedNode);
  };

  $scope.saveSvg = function saveSvg() {
    console.log('Save SVG');
    var selectedNode = $scope.selectedNode;
    nodeSelected(null);
    downloadSvg($scope.workflow.type + '.svg');
    nodeSelected(selectedNode);
  };
});


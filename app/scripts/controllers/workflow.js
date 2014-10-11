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
    return "node-" + nodeId;
  }
  function disableZoomPan() {
    var svg =  d3.select("svg");
    // panning off
    svg.on("mousedown.zoom", null);
    svg.on("mousemove.zoom", null);
    // zooming off
    svg.on("dblclick.zoom", null);
    svg.on("touchstart.zoom", null);
    svg.on("wheel.zoom", null);
    svg.on("mousewheel.zoom", null);
    svg.on("MozMousePixelScroll.zoom", null);
  }

  function higlightNode(nodeId) {
    $('#' + nodeDomId(nodeId)).css("stroke-width", "3px");
  }

  function unhiglightNode(nodeId) {
    $('#' + nodeDomId(nodeId)).css("stroke-width", "1.5px");
  }


  /** called when node is clicked */
  function nodeSelected(nodeId) {
    console.info(nodeId + " selected")
    if($scope.selectedNode) {
      unhiglightNode($scope.selectedNode);
    }
    higlightNode(nodeId);
    $scope.selectedNode = nodeId;
  };

  function drawGraphDagre(definition, canvasId) {
    var g = new dagreD3.Digraph();

    // All nodes must be in graph before edges

    // Exported SVG has silly defaults for some styles
    // so the are overridden here.
    for(var i in definition.states) {
      var state = definition.states[i];
      g.addNode(state.name, {label: state.name,
                             labelStyle: "font-size: 14px;" +
                             'font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;',
                             stroke: "black",
                             style: "stroke-width: 1.5px"
                             })
    }

    for(var i in definition.states) {
      var state = definition.states[i];
      for(var k in state.transitions){
        var transition = state.transitions[k];
        g.addEdge(null, state.name, transition,
                  {style: "stroke: black; fill: none;"});
      }
    }

    var renderer = new dagreD3.Renderer();
    var oldDrawNodes = renderer.drawNodes();
    renderer.drawNodes(
      function(g, root) {
        var svgNodes = oldDrawNodes(g, root);
        svgNodes.select("rect").attr("id",
                                  function(nodeId) {
                                    return nodeDomId(nodeId);
                                  });
        svgNodes.on("click", function(nodeId) {
          nodeSelected(nodeId)
        });
        return svgNodes;
      }
    );
    renderer.run(g, d3.select("svg g"));

    disableZoomPan();

  };

  WorkflowDefinitions.get({type: $routeParams.type},
                          function(data) {
                            var start = new Date().getTime();
                            var definition =  _.first(data);
                            $scope.workflow = definition;
                            drawGraphDagre(definition, "dagreSvg");
                            console.debug("Rendering dagre graph took " + (new Date().getTime() - start) + " msec" )
                          });

  function svgDataUrl() {
    var html = d3.select("svg")
      .attr("version", 1.1)
      .attr("xmlns", "http://www.w3.org/2000/svg")
      .node().outerHTML;
    return 'data:image/svg+xml;base64,'+ btoa(html);
  }

  /**
   * TODO doesn't work with Firefox
   */
  function downloadImage(dataurl, filename, contentType) {
    console.info("Downloading image", filename, contentType);
    var canvas = document.createElement('canvas');
    var context = canvas.getContext('2d');
    canvas.height=680
    canvas.width=650
    var image = new Image();
    image.height = 680
    image.width=650
    image.src = dataurl;
    image.onload = function(e) {
      // image load is async, must use callback
      context.drawImage(image, 0, 0);
      var canvasdata = canvas.toDataURL(contentType);
      var a = document.createElement("a");
      a.download = filename;
      a.href = canvasdata;
      a.click();
    };
    image.onerror = function(error) {
      console.error("Image downloading failed", error);
    };
  }

  function downloadSvg(filename) {
    var a = document.createElement("a");
    a.download = filename;
    a.href = svgDataUrl();
    a.click();
  }

  $scope.savePng = function savePng() {
    console.log("Save PNG");
    downloadImage(svgDataUrl(), $scope.workflow.type + ".png", "image/png")
  }

  $scope.saveSvg = function saveSvg() {
    console.log("Save SVG");
    downloadSvg($scope.workflow.type + ".svg");
  }
});


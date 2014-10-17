'use strict';

// http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
var guid = (function() {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
               .toString(16)
               .substring(1);
  }
  return function() {
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
           s4() + '-' + s4() + s4() + s4();
  };
})();

function nodeDomId(nodeId) {
  return 'node_' + nodeId;
}
function edgeDomId(edgeId) {
  return 'edge' + edgeId;
}

function nodeColors(nodeId, colors) {
  var rect = $('#' + nodeDomId(nodeId) + ' rect')
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

function workflowDefinitionGraph(definition, workflow) {
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
                           style: 'stroke-width: 1.5px;',
                           retries: 29
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

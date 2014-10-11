
  function drawGraphVisJs(definition, canvasId) {
    function buildNetwork(def) {
      // create an array with nodes
      var nodes = [];
      var edges = [];
      for(var i in def.states) {
        var state = def.states[i];
        var node = {id: state.id,
                    label: state.name,
                    title: state.description};
        if(state.type === "manual") {
          node.color = {};
          node.color.border = "black";
        }
        if(state.type === "start") {
          node.shape = "star";
        }
        if(state.type === "end") {
          node.color = {};
          node.color.border = "green";
          node.color.background = "lightgreen";
        }
        nodes.push(node);

        for(var k in state.transitions){
          var transition = state.transitions[k];
          var edge = {from: state.id,
                      to: transition,
                      style: 'arrow'};
          edges.push(edge);
        }
      }
      console.log(edges);

      var data = {
        nodes: nodes,
        edges: edges
      };
      return data;
    };
    var container = document.getElementById(canvasId);
    var options = {
      zoomable: false,
      selectable: false,
      dragNetwork: false,
      dragNodes: false,
      hierarchicalLayout: {
        enabled:false,
        //levelSeparation: 150,
        //nodeSpacing: 100,
        direction: "UD",
        layout: "direction"
      }
    };
    var network = new vis.Network(container, buildNetwork(definition), options);
    network.on("select", function(selection) {
      if(selection.nodes.length > 0) {
        nodeSelected(_.first(selection.nodes));
      }
    });

  };


  function drawGraphVizJs(definition, canvasId) {
    var container = document.getElementById(canvasId);
    var svgPrefix = "<svg xmlns='http://www.w3.org/2000/svg' width='300px' height='300px'>";
    var g = "";
    for(var i in definition.states) {
      var state = definition.states[i];

      for(var k in state.transitions){
        var transition = state.transitions[k];
        g += state.name + " -> " + transition + "; ";
      }
    }

    var svg = Viz("digraph { " + g + " }", "svg");

    //console.log(g);

    container.innerHTML = svg;
  };

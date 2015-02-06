'use strict';

function drawStateExecutionGraph(canvasId, statsData, definition, stateSelectedCallback) {
  // adapted from http://bl.ocks.org/mbostock/3886208

  function removeFinalStates() {
    var copy = _.merge({}, statsData);
    var removeNames = [];
    _.each(copy, function(state, stateName) {

      var stateDef = _.first(_.filter(definition.states, function(e) {
        return e.id === stateName;
      }));

      if(stateDef && stateDef.type === 'end') {
        removeNames.push(stateName);
      }
    });
    _.each(removeNames, function(name) {
      delete copy[name];
    });
    return copy;
  }

  var stats = removeFinalStates(statsData);
  if(Object.keys(stats).length === 0 ) {
    console.info('No data, skipping barchart drawing');
    return false;
  }

  // Remove final states
  function execTypeName(name) {
    if(name === 'nonScheduled') {
      return 'Passive';
    }
    return capitalize(name);
  }

  // Margins around image
  var margin = {top: 20, right: 120, bottom: 130, left: 40},
      width = 960 - margin.left - margin.right,
      height = 500 - margin.top - margin.bottom;

  var x = d3.scale.ordinal()
    .rangeRoundBands([0, width], 0.1);

  var y = d3.scale.linear()
    .rangeRound([height, 0]);

  var color = d3.scale.ordinal()
    .range(['#98abc5',  '#7b6888',  '#a05d56', '#ff8c00']);

  var xAxis = d3.svg.axis()
    .scale(x)
    .orient('bottom');

  var yAxis = d3.svg.axis()
    .scale(y)
    .orient('left')
    .tickFormat(d3.format('d'));

  // background
  var svgRoot = d3.select('#' + canvasId);

  // remove any previous charts
  svgRoot.selectAll('*').remove();

  var svg = svgRoot
    .attr('preserveAspectRatio', 'xMinYMin meet')
    .attr('viewBox', '0 0 ' + (width + margin.left + margin.right) + ' ' + (height + margin.top + margin.bottom))
    .append('g')
    .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

  var states = _.map(stats, function (state, stateName) {
    var r = _.merge({}, state);
    return _.merge(r, {stateName: stateName});
  });

  function getStateNames(stats, definition) {
    var nonFinalStates = _.filter(definition.states, function(state) {
      return state.type !=='end';
    });

    var defStateNames = _.map(nonFinalStates, function(state) {
      return state.name;
    });

    // stats may contain stateNames that are not part of definition
    var statStateNames = d3.keys(stats);
    return _.unique(defStateNames.concat(statStateNames));
  }

  var stateNames = getStateNames(stats, definition);
  var statNames = d3.keys(d3.values(stats)[0]).filter(function(key) { return key !== 'totalActive'; });
  color.domain(statNames);
  _.each(states, function(state) {
    var y0 = 0;
    state.execTypes = color.domain().map(function(name) {
      return {name: name, y0: y0, y1: y0 += state[name]};
    });
    state._total = state.execTypes[state.execTypes.length - 1].y1;
  });

  x.domain(stateNames);
  y.domain([0, d3.max(states, function(d) { return d._total; })]);

  // X axis
  svg.append('g')
    .attr('class', 'x axis')
    .attr('transform', 'translate(0,' + height + ')')
    .call(xAxis)
    .selectAll('text')
      .style('text-anchor', 'start')
      //.attr('dx', '-.8em')
      //.attr('dy', '.15em')
      .attr('transform', 'rotate(20)');

  // Y axis
  svg.append('g')
    .attr('class', 'y axis')
    .call(yAxis)
    .append('text');
  /*
    .attr('transform', 'rotate(-90)')
    .attr('y', 6)
    .attr('dy', '.35em')
    .style('text-anchor', 'end')
    .text('State Execution statistics');
    */

  // Stacked bars
  var state = svg.selectAll('.state')
    .data(states)
    .enter().append('g')
    .attr('class', 'g')
    .on('click', function(d) {
      stateSelectedCallback(d.stateName);
    })
    .attr('transform', function(d) { return 'translate(' + x(d.stateName) + ',0)'; });

  state.selectAll('rect')
    .data(function(d) { return d.execTypes; })
    .enter().append('rect')
    .attr('width', x.rangeBand())
    .attr('y', function(d) { return y(d.y1); })
    .attr('height', function(d) { return y(d.y0) - y(d.y1); })
    .style('fill', function(d) { return color(d.name); })
    .append('title').text(function(d) { return execTypeName(d.name) + ' ' + ( d.y1 - d.y0 ); });

  // Legend
  var legend = svg.selectAll('.legend')
    .data(color.domain().slice().reverse())
    .enter().append('g')
    .attr('class', 'legend')
    .attr('transform', function(d, i) { return 'translate(0,' + i * 20 + ')'; });

  // Legend boxes
  legend.append('rect')
    .attr('x', width)
    .attr('width', 18)
    .attr('height', 18)
    .style('fill', color);

  // Legend texts
  legend.append('text')
    .attr('x', width - 6)
    .attr('y', 9)
    .attr('dy', '.35em')
    .attr('dx', '2em')
    .style('text-anchor', 'start')
    .text(execTypeName);

  return true;
}

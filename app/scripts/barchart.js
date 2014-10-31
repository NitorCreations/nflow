'use strict';

function drawStateExecutionGraph(canvasId, stats) {
  // adapted from http://bl.ocks.org/mbostock/3886208
  /*
  var stats = {
    state1: {
      executing: 27,
      nonScheduled: 100,
      queued: 10,
      sleeping: 2,
      totalActive: 39
    },

    state2: {
      executing: 24,
      nonScheduled: 30,
      queued: 10,
      sleeping: 2,
      totalActive: 34
    },

    state3: {
      executing: 24,
      nonScheduled: 30,
      queued: 10,
      sleeping: 2,
      totalActive: 34
    },

    state4: {
      executing: 2,
      nonScheduled: 3,
      queued: 30,
      sleeping: 22,
      totalActive: 14
    },
  };
  */


  var margin = {top: 20, right: 20, bottom: 30, left: 40},
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
      .tickFormat(d3.format('.2s'));

  var svg = d3.select('#' + canvasId)
      .attr('width', width + margin.left + margin.right)
      .attr('height', height + margin.top + margin.bottom)
    .append('g')
      .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

  var states = _.map(stats, function (state, stateName) {
    var r = _.merge({}, state);
    return _.merge(r, {stateName: stateName});
  });

  var stateNames = d3.keys(stats);
  var statNames = d3.keys(d3.values(stats)[0]).filter(function(key) { return key !== 'totalActive'; });
  color.domain(statNames);
  _.each(states, function(state, stateName) {
    var y0 = 0;
    state.execTypes = color.domain().map(function(name) {
      return {name: name, y0: y0, y1: y0 += state[name]};
    });
    state._total = state.execTypes[state.execTypes.length - 1].y1;
  });

  x.domain(stateNames);
  y.domain([0, d3.max(states, function(d) { return d._total; })]);

  svg.append('g')
    .attr('class', 'x axis')
    .attr('transform', 'translate(0,' + height + ')')
    .call(xAxis);

  svg.append('g')
    .attr('class', 'y axis')
    .call(yAxis)
    .append('text')
    .attr('transform', 'rotate(-90)')
    .attr('y', 6)
    .attr('dy', '.71em')
    .style('text-anchor', 'end')
    .text('State Execution statistics');

  var state = svg.selectAll('.state')
    .data(states)
    .enter().append('g')
    .attr('class', 'g')
    .attr('transform', function(d) { return 'translate(' + x(d.stateName) + ',0)'; });

  state.selectAll('rect')
    .data(function(d) { return d.execTypes; })
    .enter().append('rect')
    .attr('width', x.rangeBand())
    .attr('y', function(d) { return y(d.y1); })
    .attr('height', function(d) { return y(d.y0) - y(d.y1); })
    .style('fill', function(d) { return color(d.name); });

  var legend = svg.selectAll('.legend')
    .data(color.domain().slice().reverse())
    .enter().append('g')
    .attr('class', 'legend')
    .attr('transform', function(d, i) { return 'translate(0,' + i * 20 + ')'; });

  legend.append('rect')
    .attr('x', width - 18)
    .attr('width', 18)
    .attr('height', 18)
    .style('fill', color);

  legend.append('text')
    .attr('x', width - 24)
    .attr('y', 9)
    .attr('dy', '.35em')
    .style('text-anchor', 'end')
    .text(function(d) { return d; });

}

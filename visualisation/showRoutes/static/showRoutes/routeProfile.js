InitChart();
function getHeightRange() {
	var min = 10000;
	var max = -10000;
	var nodes = file.nodes;
	for (var nodeId in nodes) {
		var node = nodes[nodeId];
		if (node.height<min) min = node.height;
		if (node.height>max) max = node.height;
	}
	return [min, max];
}
function getMaxDist() {
	var max = 0;
	for (var i = 0; i<file.routes.length; i++) {
		if (file.routes[i].tags.length>max) max = file.routes[i].tags.length;
	}
	return max;
}
var xScale;
var xInvScale;
var yScale;
function InitChart() {
	var vis = d3.select("#linegraph"),
    WIDTH = panel2top.offsetWidth,
    HEIGHT = panel2top.offsetHeight,
    MARGINS = {
      top: 20,
      right: 20,
      bottom: 40,
      left: 60
    };
    xScale = d3.scale.linear().range([MARGINS.left, WIDTH - MARGINS.right]).domain([0, getMaxDist()]);
    xInvScale = d3.scale.linear().range([0, getMaxDist()]).domain([MARGINS.left, WIDTH - MARGINS.right]);
    yScale = d3.scale.linear().range([HEIGHT - MARGINS.bottom, MARGINS.top]).domain(getHeightRange());
    xAxis = d3.svg.axis()
      .scale(xScale),
    yAxis = d3.svg.axis()
      .scale(yScale)
      .orient("left");
	vis.append("svg:g")
		.attr("class", "x axis")
		.attr("transform", "translate(0," + (HEIGHT - MARGINS.bottom) + ")")
		.call(xAxis);
	vis.append("svg:g")
		.attr("class", "y axis")
		.attr("transform", "translate(" + (MARGINS.left) + ",0)")
		.call(yAxis);
		
	var lineGen = d3.svg.line()
		.x(function(d) {
			return xScale(d.dist);
		})
		.y(function(d) {
			return yScale(d.height);
		});
		
	vis.selectAll().data(file.routes).enter()
		.append('path')
		.attr('d', function(route) { return lineGen(route.nodes); })
		.attr('stroke', function(route) { return route.tags.color; })
		.attr('stroke-width', 3)
		.attr('fill', 'none')
		.on("click", function(d,i) { selectedRoute = d; })
		;

	vis.append("text")
		.attr("class", "x label")
		.attr("text-anchor", "end")
		.attr("x", WIDTH - 15)
		.attr("y", HEIGHT - 6)
		.text("distance (m)");
	vis.append("text")
		.attr("class", "y label")
		.attr("text-anchor", "end")
		.attr("y", 12)
		.attr("dy", ".75em")
		.attr("transform", "rotate(-90)")
		.text("height (m)");
	vis.on('mousemove', function () {
		if (selectedRoute==undefined) return;
		var coordinates = d3.mouse(this);
		var x = xInvScale(coordinates[0]);
		if (x<0) x=0;
		if (x>selectedRoute.tags.length) x = selectedRoute.tags.length;
		addMarkers(x);
	});
}
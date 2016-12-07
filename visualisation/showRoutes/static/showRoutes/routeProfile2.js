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
function getMaxHeightDiff() {
	var max = 0;
	for (var i = 0; i<file.routes.length; i++) {
		var route = file.routes[i];
		var node = route.nodes[route.nodes.length-1]
		if (node.hd>max) max = node.hd;
	}
	return max;
}
var xScale;
var xInvScale;
var yScale;
function InitChart() {
	var vis = d3.select("#linegraph"),
    WIDTH = panel2first.offsetWidth,
    HEIGHT = panel2first.offsetHeight,
    MARGINS = {
      top: 20,
      right: 20,
      bottom: 40,
      left: 60
    };
	var vis2 = d3.select("#linegraph2"),
    WIDTH2 = panel2second.offsetWidth,
    HEIGHT2 = panel2second.offsetHeight,
    MARGINS2 = {
      top: 20,
      right: 20,
      bottom: 40,
      left: 60
    };
    xScale = d3.scale.linear().range([MARGINS.left, WIDTH - MARGINS.right]).domain([0, getMaxDist()]);
    xInvScale = d3.scale.linear().range([0, getMaxDist()]).domain([MARGINS.left, WIDTH - MARGINS.right]);
    yScale = d3.scale.linear().range([HEIGHT - MARGINS.bottom, MARGINS.top]).domain(getHeightRange());
    xScale2 = d3.scale.linear().range([MARGINS2.left, WIDTH2 - MARGINS2.right]).domain([0, getMaxDist()]);
    xInvScale2 = d3.scale.linear().range([0, getMaxDist()]).domain([MARGINS2.left, WIDTH2 - MARGINS2.right]);
    yScale2 = d3.scale.linear().range([HEIGHT2 - MARGINS2.bottom, MARGINS2.top]).domain([0, getMaxHeightDiff()]);
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
    xAxis2 = d3.svg.axis()
      .scale(xScale2),
    yAxis2 = d3.svg.axis()
      .scale(yScale2)
      .orient("left");
	vis2.append("svg:g")
		.attr("class", "x axis")
		.attr("transform", "translate(0," + (HEIGHT2 - MARGINS2.bottom) + ")")
		.call(xAxis2);
	vis2.append("svg:g")
		.attr("class", "y axis")
		.attr("transform", "translate(" + (MARGINS2.left) + ",0)")
		.call(yAxis2);
		
	var lineGen = d3.svg.line()
		.x(function(d) {
			return xScale(d.dist);
		})
		.y(function(d) {
			return yScale(d.height);
		});
		
	var lineGen2 = d3.svg.line()
		.x(function(d) {
			return xScale2(d.dist);
		})
		.y(function(d) {
			return yScale2(d.hd);
		});
		
	vis.selectAll().data(file.routes).enter()
		.append('path')
		.attr('d', function(route) { return lineGen(route.nodes); })
		.attr('stroke', function(route) { return route.tags.color; })
		.attr('stroke-width', 3)
		.attr('fill', 'none')
		.on("click", function(d,i) { selectedRoute = d; })
		;
		
	vis2.selectAll().data(file.routes).enter()
		.append('path')
		.attr('d', function(route) { return lineGen2(route.nodes); })
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
	vis2.append("text")
		.attr("class", "x label")
		.attr("text-anchor", "end")
		.attr("x", WIDTH2 - 15)
		.attr("y", HEIGHT2 - 6)
		.text("distance (m)");
	vis2.append("text")
		.attr("class", "y label")
		.attr("text-anchor", "end")
		.attr("y", 12)
		.attr("dy", ".75em")
		.attr("transform", "rotate(-90)")
		.text("cum. height diff. (m)");
	vis.on('mousemove', function () {
		if (selectedRoute==undefined) return;
		var coordinates = d3.mouse(this);
		var x = xInvScale(coordinates[0]);
		if (x<0) x=0;
		else if (x>selectedRoute.tags.length) x = selectedRoute.tags.length;
		addMarkers(x);
	});
	vis2.on('mousemove', function () {
		if (selectedRoute==undefined) return;
		var coordinates = d3.mouse(this);
		var x = xInvScale2(coordinates[0]);
		if (x<0) x=0;
		else if (x>selectedRoute.tags.length) x = selectedRoute.tags.length;
		addMarkers(x);
	});
}
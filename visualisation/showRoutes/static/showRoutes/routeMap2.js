var map;
var selectedRoute;
function processRoute(route, index) {
	// Start by processing the nodes
	route.nodes = [];
	var wayStart = route.startNode;
	for (var i = 0; i<route.ways.length; i++) {
		var way = file.ways[route.ways[i]];
		var offset;
		var direction;
		if (way.nodes[0] == wayStart) {
			offset = 0;
			direction = 1;
			wayStart = way.nodes[way.nodes.length-1];
		} else {
			offset = way.nodes.length-1;
			direction = -1;
			wayStart = way.nodes[0];
		}
		for (var j = 0; j<way.nodes.length; j++) {
			var node = file.nodes[way.nodes[offset+direction*j]];
			var tmp = new L.LatLng(node.lat, node.lng);
			tmp.height = node.height;
			tmp.way = route.ways[i];
			if (route.nodes.length!=0) {
				var prev = route.nodes[route.nodes.length-1];
				tmp.dist = prev.dist + prev.distanceTo(tmp);
				tmp.hd = prev.hd + Math.abs(tmp.height-prev.height);
			} else {
				tmp.dist = 0;
				tmp.hd = 0;
			}
			route.nodes.push(tmp);
		}
		if (way.score == undefined) {
			way.length = route.nodes[route.nodes.length-1].dist - route.nodes[route.nodes.length-way.nodes.length].dist;
			way.heightDif = route.nodes[route.nodes.length-1].hd - route.nodes[route.nodes.length-way.nodes.length].hd;
			way.score = way.linear*way.length + way.constant
			if (way.length!=0) way.normalizedScore = way.score/way.length
			delete way.constant;
			delete way.linear;
		}
	}
	// Finally, process the route
	if (route.tags === undefined) route.tags = {};
	route.tags.length = route.nodes[route.nodes.length-1].dist;
	if (index === 0) route.tags.color = 'red';
	else if (index === 1) route.tags.color = 'green';
	else if (index === 2) route.tags.color = 'blue';
	else if (index === 3) route.tags.color = 'yellow';
	else if (index === 4) route.tags.color = 'black';
}
var dashes = ['', '12,12', '6,6', '9,3', '3,9']
function processFile() {
	for (var i = 0; i<file.routes.length; i++) {
		processRoute(file.routes[i], i);
	}
}
processFile();
function map_init_basic (mp, options) {
	map = mp;
	for (var i = 0; i<file.routes.length; i++) {
		map_init_route(mp, i);
	}
	initBounds(mp);
}
function initBounds(map) {
	var minLat = 181;
	var maxLat = -181;
	var minLng = 181;
	var maxLng = -181;
	for (var nodeId in file.nodes) {
		var node = file.nodes[nodeId];
		if (node.lat>maxLat) maxLat = node.lat;
		if (node.lat<minLat) minLat = node.lat;
		if (node.lng>maxLng) maxLng = node.lng;
		if (node.lng<minLng) minLng = node.lng;
	}
	map.fitBounds([[minLat, minLng], [maxLat, maxLng]]);
}
function map_init_route(map, routeIndex) {
	var route = file.routes[routeIndex];
	var routeNodes = route.nodes;
	var startNode = route.nodes[0];
	L.marker([startNode.lat, startNode.lng]).addTo(map);
	var polyline = new L.Polyline(routeNodes, {
		color: route.tags.color,
		weight: 5,
		opacity: 1,
		smoothFactor: 1,
		dashArray: dashes[routeIndex]
	});
	polyline.routeIndex = routeIndex;
	polyline.on('mouseover', function (e) {
		clearMarkers();
	});
	polyline.on('mousemove', function (e) {
		selectedRoute = file.routes[this.routeIndex];
		var nodes = selectedRoute.nodes;
		var minDist = Infinity;
		var minPoint = null;
		var minJ = null;
		for (var j = 0; j<nodes.length-1; j++) {
			var curr =  nodes[j];
			var next = nodes[j+1];
			var d = distFromSegment(e.latlng, curr, next);
			if (d[0]<minDist) {
				minDist = d[0];
				minPoint = d[1];
				minJ = j;
			}
		}
		var curr = nodes[minJ];
		var dBefore = curr.distanceTo(minPoint);
		addMarkers(nodes[minJ].dist+dBefore);
	});
	polyline.addTo(map);
	if (route.hasOwnProperty("markpoints")) {
		var markpoints = route.markpoints;
		for (var i = 0; i<markpoints.length; i++) {
			var markpoint = markpoints[i];
			L.marker([markpoint.lat, markpoint.lon]).addTo(map);
		}
	}
}
function clearMarkers() {
	for (var i = 0; i<file.routes.length; i++) {
		var route = file.routes;
		if (route.pMap!==undefined) {
			route.pMap.closePopup();
			map.removeLayer(route.pMap);
			route.pMap = undefined;
			route.pGraph.remove();
			route.pGraph = undefined;
			route.pGraph2.remove();
			route.pGraph2 = undefined;
		}
	}
}
function tagsToString(tags, exceptions) {
	var s = "";
	for (attr in tags) {
		var value = tags[attr];
		var add = true;
		for (var i = 0; i<exceptions.length; i++) {
			if (exceptions[i] == attr) {
				add = false;
			}
		}
		if (add) {
			if (!isNaN(value)) value = Math.round(value*100)/100;
			s += "<strong> " + attr.charAt(0).toUpperCase() + attr.slice(1).replace("_", " ") + ": </strong> " + value + "<br>";
		}
	}
	return s;
}
function addMarkers(position) {
	var nodeIndex = 0;
	var dist = 0;
	var interNodeDist = selectedRoute.nodes[0].distanceTo(selectedRoute.nodes[1%selectedRoute.nodes.length]);
	while (dist+interNodeDist<position) {
		dist += interNodeDist;
		nodeIndex++;
		interNodeDist = selectedRoute.nodes[nodeIndex%selectedRoute.nodes.length].distanceTo(selectedRoute.nodes[(nodeIndex+1)%selectedRoute.nodes.length]);
	}
	var currentNode = selectedRoute.nodes[nodeIndex%selectedRoute.nodes.length];
	var nextNode = selectedRoute.nodes[(nodeIndex+1)%selectedRoute.nodes.length];
	var height = currentNode.height + (nextNode.height-currentNode.height)/interNodeDist*(position-dist);
	var heightDif = currentNode.hd + (nextNode.hd-currentNode.hd)/interNodeDist*(position-dist);
	var lat = currentNode.lat + (nextNode.lat-currentNode.lat)/interNodeDist*(position-dist);
	var lng = currentNode.lng + (nextNode.lng-currentNode.lng)/interNodeDist*(position-dist);
	var latlng = new L.LatLng(lat, lng);
	var way = file.ways[currentNode.way];
	var route = selectedRoute;
	
	var s = tagsToString(way.tags, []);
	s += tagsToString(way, ['nodes', 'tags']);
	s += "<strong> Position: </strong> " + Math.round(position*100)/100 + "<br>";
	s += "<strong> Height: </strong> " + Math.round(height*100)/100 + "<br>";
	if (route.pMap === undefined) route.pMap = L.circleMarker(latlng);
	else route.pMap.setLatLng(latlng);
	route.pMap.bindPopup(s).addTo(map).openPopup();
	var vis = d3.select("#linegraph");
	if (route.pGraph !== undefined) route.pGraph.remove();
	var color = route.tags.color;
	route.pGraph = vis
		.append("svg:circle")
		.attr("stroke", "blue")
		.attr("fill", "blue")
		.attr('fill-opacity', 0.2)
		.attr("cx", xScale(position))
		.attr("cy", yScale(height))
		.attr("r", 10);
	var vis2 = d3.select("#linegraph2");
	if (route.pGraph2 !== undefined) route.pGraph2.remove();
	route.pGraph2 = vis2
		.append("svg:circle")
		.attr("stroke", "blue")
		.attr("fill", "blue")
		.attr('fill-opacity', 0.2)
		.attr("cx", xScale2(position))
		.attr("cy", yScale2(heightDif))
		.attr("r", 10);
}
function distFromSegment(betweenNode, startNode, stopNode) {
	if (startNode.distanceTo(stopNode)<0.2) {
		return betweenNode.distanceTo(startNode);
	}
	var v1 = new L.LatLng((betweenNode.lat-startNode.lat), (betweenNode.lng-startNode.lng));
	var v2 = new L.LatLng(stopNode.lat-startNode.lat, stopNode.lng-startNode.lng);
	var projFact = (v1.lat*v2.lat+v1.lng*v2.lng)/(v2.lat*v2.lat+v2.lng*v2.lng);
	var proj = new L.LatLng(projFact*v2.lat+startNode.lat, projFact*v2.lng+startNode.lng);
	var distStart = startNode.distanceTo(betweenNode);
	var distStop = betweenNode.distanceTo(stopNode);
	var distProj = betweenNode.distanceTo(proj);
	if (distProj<distStart && distProj<distStop && 0<projFact && projFact<1) {
		return [distProj, proj];
	} else if (distStart<distStop){
		return [distStart, startNode];
	} else {
		return [distStop, stopNode];
	}
}
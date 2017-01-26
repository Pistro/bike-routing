var map;
var colors = ['red', 'green', 'yellow', 'blue', 'black'];
var dashes = ['', '12,12', '6,6', '9,3', '3,9'];
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
	route.tags.color = colors[index%colors.length];
}
function processFile() {
	console.log('Processing file...')
	for (var i = 0; i<file.routes.length; i++) {
		processRoute(file.routes[i], i);
	}
	console.log('File processed!')
}
processFile();
function map_init_basic (mp, options) {
	initBounds(mp);
	map = mp;
	for (var i = 0; i<file.routes.length; i++) {
		map_init_route(mp, i);
	}
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
		dashArray: dashes[routeIndex%dashes.length]
	});
	polyline.on('click', function (e) {
		var minDist = Infinity;
		var minPoint = null;
		var minJ = null;
		for (var j = 0; j<routeNodes.length-1; j++) {
			var curr =  routeNodes[j];
			var next = routeNodes[j+1];
			var d = distFromSegment(e.latlng, curr, next);
			if (d[0]<minDist) {
				minDist = d[0];
				minPoint = d[1];
				minJ = j;
			}
		}
		var curr = routeNodes[minJ];
		var dBefore = curr.distanceTo(minPoint);
		addMarker(route, routeNodes[minJ].dist+dBefore);
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
var marker;
function addMarker(route, position) {
	var nodeIndex = 0;
	var dist = 0;
	var interNodeDist = route.nodes[0].distanceTo(route.nodes[1%route.nodes.length]);
	while (dist+interNodeDist<position) {
		dist += interNodeDist;
		nodeIndex++;
		interNodeDist = route.nodes[nodeIndex%route.nodes.length].distanceTo(route.nodes[(nodeIndex+1)%route.nodes.length]);
	}
	var currentNode = route.nodes[nodeIndex%route.nodes.length];
	var nextNode = route.nodes[(nodeIndex+1)%route.nodes.length];
	var lat = currentNode.lat + (nextNode.lat-currentNode.lat)/interNodeDist*(position-dist);
	var lng = currentNode.lng + (nextNode.lng-currentNode.lng)/interNodeDist*(position-dist);
	var latlng = new L.LatLng(lat, lng);
	var way = file.ways[currentNode.way];
	
	var s = tagsToString(way.tags, []);
	s += tagsToString(way, ['nodes', 'tags']);
	s += "<strong> Position: </strong> " + Math.round(position*100)/100 + "<br>";
	if (marker === undefined) marker = L.circleMarker(latlng);
	else marker.setLatLng(latlng);
	marker.bindPopup(s).addTo(map).openPopup();
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
var map;
var colors = ['red', 'green', 'yellow', 'blue', 'black'];
var dashes = ['', '12,12', '6,6', '9,3', '3,9'];
var mapObjects = [];

function loadFile() {
	if (typeof window.FileReader !== 'function') {
		alert("The file API isn't supported on this browser yet.");
		return;
	}
	input = document.getElementById('fileinput');
    if (!input)  alert("Um, couldn't find the fileinput element.");
	else if (!input.files) alert("This browser doesn't seem to support the `files` property of file inputs.");
	else if (!input.files[0]) alert("Please select a file before clicking 'Load'");
    else {
		var fr = new FileReader();
		fr.onload = receivedText;
		fr.readAsText(input.files[0]);
    }
	function receivedText(e) {
		var file;
		try {
			console.log("Parsing JSON...")
			file = JSON.parse(e.target.result);
		} catch (e) {
			console.log(e);
			alert("Invalid JSON file.");
			return;
		}
		console.log('Processing file...')
		clearMap();
		initBounds(file);
		for (var i = 0; i<file.routes.length; i++) {
			addRouteToMap(file.routes[i], i);
		}
		addTags(file)
		console.log('File processed!')
	}
}

function clearMap() {
	for (object in mapObjects) {
		map.removeLayer(mapObjects[object]);
	}
	mapObjects = [];
}

function addRouteToMap(route, index) {
	// Start by processing the nodes
	var nodes = [];
	for (var i = 0; i<route.nodes.length; i++) {
		var node = route.nodes[i];
		nodes.push(new L.LatLng(node.lat, node.lon));
	}
	if (!route.hasOwnProperty("tags")) route.tags = {};
	route.tags.color = colors[index%colors.length];
	var polyline = new L.Polyline(nodes, {
		color: route.tags.color,
		weight: 5,
		opacity: 1,
		smoothFactor: 1,
		dashArray: dashes[index%dashes.length]
	});
	polyline.addTo(map);
	mapObjects.push(polyline)
	if (route.hasOwnProperty("markpoints")) {
		var markpoints = route.markpoints;
		for (var i = 0; i<markpoints.length; i++) {
			var markpoint = markpoints[i];
			var marker = L.marker([markpoint.lat, markpoint.lon]);
			marker.addTo(map);
			mapObjects.push(marker);
		}
	}
}

function initBounds(file) {
	var minLat = 181;
	var maxLat = -181;
	var minLon = 181;
	var maxLon = -181;
	for (var i = 0; i<file.routes.length; i++) {
		var route = file.routes[i];
		for (var j = 0; j<route.nodes.length; j++) {
			var node = route.nodes[j];
			if (node.lat>maxLat) maxLat = node.lat;
			if (node.lat<minLat) minLat = node.lat;
			if (node.lon>maxLon) maxLon = node.lon;
			if (node.lon<minLon) minLon = node.lon;
		}
	}
	map.fitBounds([[minLat, minLon], [maxLat, maxLon]]);
}

function addTags(file) {
	if (file.tags == undefined) file.tags = {};
	if (file.tags.name == undefined) file.tags.name = "Details";
	var info = "<h2>" + file.tags.name + "</h2>" + tagsToString(file.tags, ["name"]);
	for (var i = 0; i<file.routes.length; i++) {
		var route = file.routes[i];
		if (route.tags == undefined) route.tags = {};
		var routeTags = route.tags;
		info += "<p><h3>" + (routeTags.name == undefined? "Tour " + i : routeTags.name) + "</h3>" + tagsToString(routeTags, ["name"]) + "</p>";
	}
	details.innerHTML = info;
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

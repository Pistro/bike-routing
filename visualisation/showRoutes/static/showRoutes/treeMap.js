var map;
var wayShown;
var colors = ['#236693', '#33A02C', '#FB9A99', 'blue', 'black'];
function processTree(tree, index) {
	if (tree.tags == undefined) tree.tags = {};
	tree.tags.color = colors[index%colors.length];
}
function processFile() {
	for (var i = 0; i<file.trees.length; i++) {
		processTree(file.trees[i], i);
	}
}
processFile();
function map_init_basic (mp, options) {
	map = mp;
	L.geoJson(belgiumBorder, {style: {
		color: "black",
		weight: 2,
		opacity: 1,
		fillOpacity: 0.5,
		fillColor: "white"
	}}).addTo(map);
	for (var i = 0; i<file.trees.length; i++) {
		map_init_tree(mp, i);
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
function map_init_tree(map, treeIndex) {
	var tree = file.trees[treeIndex];
	var treeWays = tree.ways;
	for (var i = 0; i<treeWays.length; i++) {
		drawWay(treeWays[i], tree.tags.color)
	}
	var treeStarts = tree.starts;
	for (var i = 0; i<treeStarts.length; i++) {
		var node = file.nodes[treeStarts[i]];
		L.marker([node.lat, node.lng]).addTo(map);
	}
}
function drawWay(wayIndex, c) {
	var way = file.ways[wayIndex];
	var wayNodes = way.nodes;
	var latlngs = [];
	for (var j = 0; j<wayNodes.length; j++) {
		node = file.nodes[wayNodes[j]];
		latlngs.push([parseFloat(node.lat), parseFloat(node.lng)])
	}
	polyline = L.polyline(latlngs, {color: c, opacity: 1}).addTo(map);
	polyline.on('mousemove', showWayInfo);
	polyline.index = wayIndex;
}
function showWayInfo(e) {
	var index = this.index;
	if (index != wayShown) {
		wayShown = index;
		var way = file.ways[index];
		var out = "<h3>Attributes</h3>";
		out += tagsToString(way, ['tags', 'nodes'])
		out += "<h3>Tags</h3>";
		out += tagsToString(way.tags, [])
		details.innerHTML = out;
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
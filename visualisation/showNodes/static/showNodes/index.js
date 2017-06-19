var maxScore;
var minScore;
var scoreAtt;
var map;
var nodes;
var addedNodes = {};
var nodeShown;
var markers = [];
var addedMarkers = [];
var topBorder;

// Initialize
function initialize(file) {
	console.log('Start!')
	var startNodeTime = new Date().getTime();
	initNodes(file);
	var endNodeTime = new Date().getTime();
	console.log('NodeDict initialized: ' + (endNodeTime - startNodeTime)/1000 + 's')
	if (file.hasOwnProperty("markpoints")) {
		markers = file.markpoints;
	}
	reinitializeMap();
}

function reinitializeMap() {
	minScore = parseFloat(document.getElementById('minScoreInput').value);
	maxScore = parseFloat(document.getElementById('maxScoreInput').value);
	scoreAtt = document.getElementById('scoreAttrInput').value;
	minBound.innerHTML = minScore;
	maxBound.innerHTML = maxScore;
	scoreAttr.innerHTML = scoreAtt;
	for (attr in addedNodes) {
		map.removeLayer(addedNodes[attr]);
	}
	if (topBorder) map.removeLayer(topBorder)
	addedNodes = {};
	for (var i = 0; i<addedMarkers.length; i++) {
		map.removeLayer(addedMarkers[i]);
	}
	addedMarkers = [];
	nodeShown = null;
	details.innerHTML = "No node selected.";
	drawMap();
}

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
		initialize(file)
	}
}

function map_init_basic(mp, options) {
	map = mp;
	L.geoJson(belgiumBorder, {"style": {
		"color": "black",
		"weight": 2,
		"opacity": 1,
		"fillOpacity": 0.7,
		"fillColor": "white"
	}}).addTo(map);
}

function drawMap(e) {
	console.log("Drawing nodes...")
	for (var nodeId in nodes) {
		drawNode(nodes[nodeId]);
	}
	console.log("Nodes drawn!")
	for (var i = 0; i<markers.length; i++) {
		var markpoint = markers[i];
		var marker = L.marker([markpoint.lat, markpoint.lon]).addTo(map);
		addedMarkers.push(marker);
	}
	topBorder = L.geoJson(belgiumBorder, {"style": {
		"color": "black",
		"weight": 2,
		"opacity": 1,
		"fillOpacity": 0
	}}).addTo(map);
}
	
function drawNode(node) {
	var c;
	if (node.hasOwnProperty(scoreAtt)) {
		col = numberToColorHsl(parseFloat(node[scoreAtt]), minScore, maxScore),
		c = L.circle([node.lat, node.lon], 20, {color: col, opacity: 1, fillColor: col, fillOpacity: 1}).addTo(map);
		c.on('mousemove', showNodeInfo);
		c.index = node.id;
	} else {
		c = {};
	}
	addedNodes[node.id] = c;
}

function showNodeInfo(e) {
	var index = this.index;
	if (index != nodeShown) {
		nodeShown = index;
		var node = nodes[index];
		var out = "<h3>Attributes</h3>";
		for (attr in node) {
			if (attr != 'nd' && attr.substring(0, 4) != "tag_") {
				out += "<strong>" + attr + ":</strong> " + node[attr] + "<br>";
			}
		}
		out += "<h3>Tags</h3>";
		for (attr in node) {
			if (attr.substring(0, 4) == "tag_") {
				out += "<strong>" + attr.slice(4) + ":</strong> " + node[attr] + "<br>";
			}
		}
		details.innerHTML = out;
	}
}

function initNodes(file) {
	var nodesList;
	if (file.hasOwnProperty ('nodes')) {
		nodesList = file.nodes;
	} else nodesList = [];
	nodes = {}
	var minLat = 180;
	var minLon = 180;
	var maxLat = -180;
	var maxLon = -180;
	for (var i = 0; i<nodesList.length; i++) {
		var node = nodesList[i]
		nodes[node.id] = node
		if (node.lat<minLat) minLat = node.lat;
		if (node.lat>maxLat) maxLat = node.lat;
		if (node.lon<minLon) minLon = node.lon;
		if (node.lon>maxLon) maxLon = node.lon;
	}
	map.fitBounds([[minLat, minLon], [maxLat, maxLon]]);
}

// convert a number to a color using hsl
function numberToColorHsl(score, mi, ma) {
	score = (score - mi)/(ma - mi)
	if (score>1) score = 1;
	if (score<0) score = 0;
	// as the function expects a value between 0 and 1, and red = 0° and green = 120°
    // we convert the input to the appropriate hue value
    var hue = (169+(144-169)*score)/360;
	var saturation = (46+(100-46)*score)/360;
	var lightness = (86+(13-86)*score)/100;
    // we convert hsl to rgb (saturation 100%, lightness 50%)
    var rgb = hslToRgb(hue, saturation, lightness);
    // we format to css value and return
    return 'rgb(' + rgb[0] + ',' + rgb[1] + ',' + rgb[2] + ')'; 
}

/**
 * http://stackoverflow.com/questions/2353211/hsl-to-rgb-color-conversion
 *
 * Converts an HSL color value to RGB. Conversion formula
 * adapted from http://en.wikipedia.org/wiki/HSL_color_space.
 * Assumes h, s, and l are contained in the set [0, 1] and
 * returns r, g, and b in the set [0, 255].
 *
 * @param   Number  h       The hue
 * @param   Number  s       The saturation
 * @param   Number  l       The lightness
 * @return  Array           The RGB representation
 */
function hslToRgb(h, s, l){
    var r, g, b;

    if(s == 0){
        r = g = b = l; // achromatic
    }else{
        function hue2rgb(p, q, t){
            if(t < 0) t += 1;
            if(t > 1) t -= 1;
            if(t < 1/6) return p + (q - p) * 6 * t;
            if(t < 1/2) return q;
            if(t < 2/3) return p + (q - p) * (2/3 - t) * 6;
            return p;
        }

        var q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        var p = 2 * l - q;
        r = hue2rgb(p, q, h + 1/3);
        g = hue2rgb(p, q, h);
        b = hue2rgb(p, q, h - 1/3);
    }

    return [Math.floor(r * 255), Math.floor(g * 255), Math.floor(b * 255)];
}
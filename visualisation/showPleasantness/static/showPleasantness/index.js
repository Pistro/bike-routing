var maxScore;
var minScore;
var scoreAtt;
var map;
var nodes;
var ways;
var wayTree;
var addedWays = {};
var wayShown;

// Initialize
function initialize(file) {
	console.log('Start!')
	var startNodeTime = new Date().getTime();
	initNodes(file);
	var endNodeTime = new Date().getTime();
	console.log('NodeDict initialized: ' + (endNodeTime - startNodeTime)/1000 + 's')
	if (file.hasOwnProperty ('way')) {
		ways = file.way;
	} else ways = [];
	initTree(file);
	map.fitBounds([[file.bounds.minlat, file.bounds.minlon], [file.bounds.maxlat, file.bounds.maxlon]]);
	map.on('moveend', drawMap);
	reinitializeMap();
}

function reinitializeMap() {
	var minSc = parseFloat(document.getElementById('minScoreInput').value);
	var maxSc = parseFloat(document.getElementById('maxScoreInput').value);
//	if (maxSc<minSc) {
//		alert('Maxscore should be larger than minscore!')
//		return;
//	}
	minScore = minSc;
	maxScore = maxSc;
	scoreAtt = document.getElementById('scoreAttrInput').value;
	minBound.innerHTML = minSc;
	maxBound.innerHTML = maxSc;
	scoreAttr.innerHTML = scoreAtt;
	for (attr in addedWays) {
		map.removeLayer(addedWays[attr]);
	}
	addedWays = {};
	wayShown = null;
	details.innerHTML = "No way selected.";
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
}

function drawMap(e) {
	if (map.getZoom()>13) {
		console.log("Retrieving ways ways...")
		var bounds = map.getBounds();
		var queryResults = wayTree.search([bounds.getSouth(), bounds.getWest(), bounds.getNorth(), bounds.getEast()]);
		if (queryResults.length<3000) {
			console.log("Drawing ways...")
			for (var i = 0; i<queryResults.length; i++) {
				drawWay(queryResults[i][4].index);
			}
			console.log("Ways drawn!")
		} else {
			console.log("Too many ways in this region to draw, zoom further in.")
		}
	} else {
		console.log("Zoomed out to far to draw ways.")
	}
}
	
function drawWay(wayIndex) {
	if (addedWays.hasOwnProperty(wayIndex)) {
		return;
	}
	var way = ways[wayIndex];
	var wayNodes = way.nd;
	var latlngs = [];
	for (var j = 0; j<wayNodes.length; j++) {
		latlngs.push([parseFloat(nodes[wayNodes[j]][0]), parseFloat(nodes[wayNodes[j]][1])])
	}
	var polyline;
	if (way.hasOwnProperty(scoreAtt)) {
		polyline = L.polyline(latlngs, {color: numberToColorHsl(parseFloat(way[scoreAtt]), minScore, maxScore) }).addTo(map);
		polyline.on('mousemove', showWayInfo);
		polyline.index = wayIndex;
	} else {
		polyline = {};
	}
	addedWays[wayIndex] = polyline;
}

function showWayInfo(e) {
	var index = this.index;
	if (index != wayShown) {
		wayShown = index;
		var way = ways[index];
		var out = "<h3>Attributes</h3>";
		for (attr in way) {
			if (attr != 'nd' && attr.substring(0, 4) != "tag_") {
				out += "<strong>" + attr + ":</strong> " + way[attr] + "<br>";
			}
		}
		out += "<h3>Tags</h3>";
		for (attr in way) {
			if (attr.substring(0, 4) == "tag_") {
				out += "<strong>" + attr.slice(4) + ":</strong> " + way[attr] + "<br>";
			}
		}
		details.innerHTML = out;
	}
}

function initNodes(file) {
	var nodesList;
	if (file.hasOwnProperty ('node')) {
		nodesList = file.node;
	} else nodesList = [];
	nodes = {}
	for (var i = 0; i<nodesList.length; i++) {
		var node = nodesList[i]
		nodes[node.id] = [node.lat, node.lon]
	}
}

function initTree(file) {
	wayTree = rbush(10);
	var startColTime = new Date().getTime();
	var wayBounds = [];
	for (var i = 0; i<ways.length; i++) {
		var way = ways[i];
		if (way.hasOwnProperty ('nd') && way.nd.length>0) {
			var wayNodes = ways.nd;
			var minLat = Infinity, maxLat = -Infinity, minLon = Infinity, maxLon = -Infinity;
			for (var j = 0; j<way.nd.length; j++) {
				var node = nodes[way.nd[j]]
				if (node[0]<minLat) minLat = node[0];
				if (node[0]>maxLat) maxLat = node[0];
				if (node[1]<minLon) minLon = node[1];
				if (node[1]>maxLon) maxLon = node[1];
			}
			wayBounds.push([minLat, minLon, maxLat, maxLon, {index: i}])
		}
	}
	var endColTime = new Date().getTime();
	console.log('Tree data collected: ' + (endColTime - startColTime)/1000 + 's')
	var startTreeTime = new Date().getTime();
	wayTree.load(wayBounds)
	var endTreeTime = new Date().getTime();
	console.log('Tree initialized: ' + (endTreeTime - startTreeTime)/1000 + 's')
}

// convert a number to a color using hsl
function numberToColorHsl(score, mi, ma) {
	score = (score - mi)/(ma - mi)
	if (score>1) score = 1;
	if (score<0) score = 0;
	// as the function expects a value between 0 and 1, and red = 0° and green = 120°
    // we convert the input to the appropriate hue value
    var hue = score * 120 / 360;
    // we convert hsl to rgb (saturation 100%, lightness 50%)
    var rgb = hslToRgb(hue, 1, .5);
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
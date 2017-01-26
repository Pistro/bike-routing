if (file.tags == undefined) file.tags = {};
if (file.tags.name == undefined) file.tags.name = "Test";
title.innerHTML = file.tags.name;
var info = "<h2>" + file.tags.name + "</h2>" + tagsToString(file.tags, ["name"]);
for (var i = 0; i<file.routes.length; i++) {
	var route = file.routes[i];
	if (route.tags == undefined) route.tags = {};
	var routeTags = route.tags;
	info += "<p><h3>" + (routeTags.name == undefined? "Tour " + i : routeTags.name) + "</h3>" + tagsToString(routeTags, ["name"]) + "</p>";
}
panel2.innerHTML = info;
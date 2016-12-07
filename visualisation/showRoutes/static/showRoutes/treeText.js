if (file.tags == undefined) file.tags = {};
if (file.tags.name == undefined) file.tags.name = "Trees";
treeTitle.innerHTML = file.tags.name;
var info = tagsToString(file.tags, ["name"]);
for (var i = 0; i<file.trees.length; i++) {
	var tree = file.trees[i];
	if (tree.tags == undefined) tree.tags = {};
	var treeTags = tree.tags;
	info += "<p><h3>" + (treeTags.name == undefined? "Tree " + i : treeTags.name) + "</h3>" + tagsToString(treeTags, ["name"]) + "</p>";
}
treeInfo.innerHTML = info;
createWgr () {
python3 osm2graph \
	--verbose \
	--clean-way \
	--step \
	--weights profile=bikeProfile \
	--filter rules=+*,-osm.node.attr.version,-osm.node.attr.timestamp,-osm.way:allows_bikes~0,-osm.way.attr.allows_bikes,-osm.way.attr.version,-osm.way.attr.timestamp,-osm.bounds,-osm.way.nd \
	--xml out=../source/$1.wgr ../source/$1_inter.osm
}

createWgr $1
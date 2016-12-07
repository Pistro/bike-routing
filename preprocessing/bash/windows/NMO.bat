call osmosis ^
  --read-pbf ..\source\belgium.osm.pbf ^
  --tf reject-relations ^
  --tf accept-ways highway=* ^
  --used-node ^
  --write-xml ..\source\belgium_A.osm

call python osm2graph ^
    --verbose ^
	--way-to-edge ^
	--step ^
	--weights profile=bikeProfile poiWeight=1 qWeight=1 ^
	--filter rules=+*,-osm.node.*,+osm.node.attr.id,+osm.node.attr.lat,+osm.node.attr.lon,+osm.node.attr.version,+osm.node.attr.timestamp,-osm.way,+osm.way:allows_bikes~1,+osm.way.attr.id,+osm.way.attr.timestamp,+osm.way.attr.version,+osm.way.nd,+osm.way.tag:k~bicycle_oneway,+osm.way.tag:k~original_id ^
	--clean-node ^
	--step ^
	--xml out=..\source\belgium_B.osm ..\source\belgium_A.osm
	
call osmosis ^
  --read-xml ..\source\belgium_B.osm ^
  --bounding-box top=51.15 left=3.65 bottom=51 right=3.85 ^
  --write-xml ..\source\ghent.osm
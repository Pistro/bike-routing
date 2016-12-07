osm2graph () {
python3 osm2graph \
    --verbose \
	--inherit remap=route.bicycle.bikeways,route.foot.footways,route.running.footways,route.hiking.footways,route.mtb.mtbways,route.MTB.mtbways \
	--way-to-edge \
	--step \
	--filter rules=+*,-osm.relation,-osm.node.*,+osm.node.attr.lat,+osm.node.attr.lon,+osm.node.attr.id,+osm.node.attr.version,+osm.node.attr.timestamp,-osm.way.attr.*,+osm.way.attr.id,+osm.way.attr.version,+osm.way.attr.timestamp \
	--xml out=../source/$1_edge.osm ../source/$1_way.osm


python3 osm2graph \
	--verbose \
	--poi in=../source/$1_poi.osm remap=amenity.bar,amenity.bbq,amenity.biergarten,amenity.cafe,amenity.drinking_water,amenity.fast_food,amenity.ice_cream,amenity.pub,amenity.restaurant,amenity.bicycle_parking,amenity.bicycle_repair_station,amenity.bicycle_rental,amenity.car_wash,amenity.charging_station,amenity.fuel,amenity.parking.car_parking,amenity.fountain,amenity.place_of_worship,amenity.toilets \
	--pass-through in=../source/$1_forest_way.osm remap=landuse.forest,natural.wood \
	--pass-through rel in=../source/$1_forest_rel.osm remap=landuse.forest,natural.wood \
	--pass-near in=../source/$1_water.osm tag=waterside \
	--pass-near in=../source/$1_primary.osm tag=primaryside \
	--pass-near in=../source/$1_secondary.osm tag=secondaryside \
	--pass-near in=../source/$1_tertiary.osm tag=tertiaryside \
	--cross endings remap=highway.primary.highway_primary,highway.secondary.highway_secondary,highway.tertiary.highway_tertiary \
	--step \
	--edge-info \
	--height in=../source/$1.tif \
	--xml out=../source/$1_inter.osm \
	--filter rules=+*,-osm.bounds,-osm.node.attr.version,-osm.node.attr.timestamp,-osm.way.attr.version,-osm.way.attr.timestamp,-osm.way.tag:k~length,-osm.way.tag:k~height_dif,-osm.way.tag:k~start_node,-osm.way.tag:k~end_node \
	--fix out=../source/$1_fixture.xml ../source/$1_edge.osm
}

osm2graph $1
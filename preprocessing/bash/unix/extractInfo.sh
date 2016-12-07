extractInfo () {
osmosis \
  --read-pbf ../source/$1.osm.pbf \
  --tf accept-relations route=bicycle,foot,running,hiking,mtb,MTB \
  --tf accept-ways highway=* \
  --used-node \
  --write-xml ../source/$1_way.osm

osmosis \
  --read-pbf ../source/$1.osm.pbf \
  --tf reject-relations \
  --tf accept-ways highway=primary \
  --used-node \
  --write-xml ../source/$1_primary.osm
  
osmosis \
  --read-pbf ../source/$1.osm.pbf \
  --tf reject-relations \
  --tf accept-ways highway=secondary \
  --used-node \
  --write-xml ../source/$1_secondary.osm
  
osmosis \
  --read-pbf ../source/$1.osm.pbf \
  --tf reject-relations \
  --tf accept-ways highway=tertiary \
  --used-node \
  --write-xml ../source/$1_tertiary.osm
  
osmosis \
  --read-pbf ../source/$1.osm.pbf \
  --tf accept-nodes amenity=bar,bbq,biergarten,cafe,drinking_water,fast_food,food_court,ice_cream,pub,restaurant,bicycle_parking,bicycle_repair_station,bicycle_rental,car_wash,charging_station,fuel,parking,fountain,place_of_worship,toilets \
  --tf reject-ways \
  --tf reject-relations outPipe.0="nodePipe" \
  --read-pbf ../source/$1.osm.pbf \
  --tf accept-ways amenity=bar,biergarten,cafe,fast_food,food_court,ice_cream,pub,restaurant,bicycle_parking,bicycle_repair_station,bicycle_rental,car_wash,fuel,parking,fountain,place_of_worship,toilets \
  --tf reject-relations \
  --used-node outPipe.0="wayPipe" \
  --merge inPipe.0="nodePipe" inPipe.1="wayPipe" \
  --write-xml ../source/$1_poi.osm
  
osmosis \
  --read-pbf ../source/$1.osm.pbf \
  --tf reject-relations \
  --way-key-value keyValueList="landuse.forest,natural.wood" \
  --used-node \
  --write-xml ../source/$1_forest_way.osm
  
osmosis \
  --read-pbf ../source/$1.osm.pbf \
  --tf accept-relations landuse=forest \
  --used-way \
  --used-node outPipe.0="forestPipe" \
  --read-pbf ../source/$1.osm.pbf \
  --tf accept-relations natural=wood \
  --used-way \
  --used-node outPipe.0="woodPipe" \
  --merge inPipe.0="forestPipe" inPipe.1="woodPipe" \
  --write-xml ../source/$1_forest_rel.osm
  
osmosis \
  --read-pbf ../source/$1.osm.pbf \
  --tf reject-relations \
  --tf accept-ways natural=water \
  --used-node outPipe.0="naturalPipe" \
  --read-pbf ../source/$1.osm.pbf \
  --tf reject-relations \
  --tf accept-ways landuse=reservoir \
  --used-node outPipe.0="reservoirPipe" \
  --read-pbf ../source/$1.osm.pbf \
  --tf reject-relations \
  --tf accept-ways waterway=* \
  --used-node outPipe.0="waterPipe" \
  --merge inPipe.0="naturalPipe" inPipe.1="reservoirPipe" outPipe.0="mergePipe" \
  --merge inPipe.0="mergePipe" inPipe.1="waterPipe" \
  --write-xml ../source/$1_water.osm
}

extractInfo $1
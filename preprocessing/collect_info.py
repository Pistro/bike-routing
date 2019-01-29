import subprocess
import os.path

def collect_info(infile, tmpfolder, tmpprefix, outfile):
	subprocess.call(['osmosis',
		'--read-pbf', infile,
		'--tf', 'accept-relations', 'route=bicycle,foot,running,hiking,mtb,MTB',
		'--tf', 'accept-ways', 'highway=*',
		'--used-node',
		'--write-xml', os.path.join(tmpfolder, tmpprefix + '_way.osm')], shell=True)
		
	subprocess.call(['osmosis',
		'--read-pbf', infile,
		'--tf', 'reject-relations',
		'--tf', 'accept-ways', 'highway=primary,secondary,tertiary',
		'--used-node',
		'--write-xml', os.path.join(tmpfolder, tmpprefix + '_busy_ways.osm')], shell=True)
		
	subprocess.call(['osmosis',
		'--read-pbf', infile,
		'--tf', 'accept-nodes', 'amenity=bar,bbq,biergarten,cafe,drinking_water,fast_food,food_court,ice_cream,pub,restaurant,bicycle_parking,bicycle_repair_station,bicycle_rental,car_wash,charging_station,fuel,parking,fountain,place_of_worship,toilets',
		'--tf', 'reject-ways',
		'--tf', 'reject-relations', 'outPipe.0="nodePipe"',
		'--read-pbf', infile,
		'--tf', 'accept-ways', 'amenity=bar,biergarten,cafe,fast_food,food_court,ice_cream,pub,restaurant,bicycle_parking,bicycle_repair_station,bicycle_rental,car_wash,fuel,parking,fountain,place_of_worship,toilets',
		'--tf', 'reject-relations',
		'--used-node', 'outPipe.0="wayPipe"',
		'--merge', 'inPipe.0="nodePipe"', 'inPipe.1="wayPipe"',
		'--write-xml', os.path.join(tmpfolder, tmpprefix + '_poi.osm')], shell=True)
		
	subprocess.call(['osmosis',
		'--read-pbf', infile,
		'--tf', 'reject-relations',
		'--tf', 'accept-ways', 'landuse=forest', 'natural=wood',
		'--used-node', 'outPipe.0="wayPipe"',
		'--read-pbf', infile,
		'--tf', 'accept-relations', 'landuse=forest', 'natural=wood',
		'--used-way',
		'--used-node', 'outPipe.0="relPipe"',
		'--merge', 'inPipe.0="wayPipe"', 'inPipe.1="relPipe"',
		'--write-xml', os.path.join(tmpfolder, tmpprefix + '_forest.osm')], shell=True)
		
	subprocess.call(['osmosis',
		'--read-pbf', infile,
		'--tf', 'reject-relations',
		'--tf', 'accept-ways', 'natural=water', 'landuse=reservoir', 'waterway=*',
		'--used-node',
		'--write-xml', os.path.join(tmpfolder, tmpprefix + '_water.osm')], shell=True)
		
	subprocess.call(['java', '-jar', 'osm2graph.jar',
		'-verbose',
		'--inherit-tag', 'map=route~bicycle2bikeways,route~foot2footways,route~running2footways,route~hiking2footways,route~mtb2mtbways,route~MTB2mtbways',
		'--way-to-edge',
		'--length',
		'--filter', 'rules=-relation,-node:,+node:id,+node:lat,+node:lon,+node:version,+node:timestamp,-node.,-way:,+way:id,+way:id_old,+way:version,+way:timestamp',
		'--pass-closest', 'in=' + os.path.join(tmpfolder, tmpprefix + '_poi.osm'), 'map=amenity~bar2drink,amenity~bbq2eat,amenity~biergarten2drink,amenity~cafe2drink,amenity~drinking_water2drink,amenity~fast_food2eat,amenity~ice_cream2eat,amenity~pub2drink,amenity~restaurant2eat,amenity~bicycle_parking2bicycle_parking,amenity~bicycle_repair_station2bicycle_repair,amenity~bicycle_rental2bicycle_rent,amenity~fountain2poi,amenity~place_of_worship2poi,amenity~toilets2toilets',
		'--pass-through', 'in=' + os.path.join(tmpfolder, tmpprefix + '_forest.osm'), 'map=landuse~forest2treeland,natural~wood2treeland',
		'--pass-near', 'in=' + os.path.join(tmpfolder, tmpprefix + '_water.osm'), 'map=natural~water2waterside,landuse~reservoir2waterside,waterway~*2waterside',
		'--pass-near', 'in=' + os.path.join(tmpfolder, tmpprefix + '_busy_ways.osm'), 'map=highway~primary2primaryside,highway~secondary2secondaryside,highway~tertiary2tertiaryside',
		'--xml', 'out=' + outfile,
		os.path.join(tmpfolder, tmpprefix + '_way.osm')])

collect_info(
	infile=os.path.join('..', 'data', 'belgium-latest.osm.pbf'),
	tmpfolder=os.path.join('..', 'inter'),
	tmpprefix='belgium',
	outfile=os.path.join('..', 'graph', 'belgium_unweighted.osm')
	)
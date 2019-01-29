from xml.sax import make_parser
from xml.sax.saxutils import XMLFilterBase, XMLGenerator
import os.path
import math

paved  = {'paved', 'asphalt', 'cobblestone', 'sett', 'concrete', 'concrete:lanes', 'concrete:plates', 'paving_stones', 'metal', 'gravel'}

def getConstantScoreSafe(tags):
	score = 0
	if ('primary_intersection' in tags):
		score += 200 * int(tags['primary_intersection'])
	if ('secondary_intersection' in tags):
		score += 100 * int(tags['secondary_intersection'])
	if ('tertiary_intersection' in tags):
		score += 50 * int(tags['tertiary_intersection'])
	return score

def getLinearScoreSafe(tags):
	highwayScores = {'primary': 4, 'primary_link': 4, 'secondary': 3, 'secondary_link': 3, 'tertiary': 2, 'tertiary_link': 2, 'unclassified': 1, 'service': 1, 'residential': 1, 'living_street': 0.7, 'cycleway': 0.5}
	highwayType = tags['highway']
	if (highwayType in highwayScores):
		score = highwayScores[highwayType]
	elif (highwayType == 'pedestrian'):
		if ('bicycle' in tags and (tags['bicycle'] == 'yes' or tags['bicycle'] == 'designated')):
			score = 0.5
		else:
			score = 0.7
	elif (highwayType == 'path' or highwayType == 'footway' or highwayType == 'track'):
		if (('surface' in tags and tags['surface'] in paved) or ('bicycle' in tags and (tags['bicycle'] == 'yes' or tags['bicycle'] == 'designated'))):
			score = 0.5
		else:
			score = 1.5
	else:
		score = 1
				
	if ('cycleway' in tags):
		cycleWayType = tags['cycleway']
		if (cycleWayType == 'lane' or cycleWayType == 'opposite_lane'):
			score -= 1
		elif (cycleWayType == 'track' or cycleWayType == 'opposite_track'):
			score = 0.5
		
	return max(score, 0.5)

def getConstantScoreAttr(tags):
	score = 0
	if 'poi' in tags:
		score -= 90*int(tags['poi'])
	if 'toilets' in tags:
		score -= 50
	if ('eat' in tags) or ('drink' in tags):
		score -= 100
	if 'bicycle_parking' in tags:
		score -= 70
	return score

def getLinearScoreAttr(tags):
	highwayType = tags['highway']
	highwayScores = {'primary': 4, 'primary_link': 4, 'secondary': 3, 'secondary_link': 3, 'tertiary': 2, 'tertiary_link': 2, 'service': 1, 'residential': 1, 'unclassified': 1, 'living_street': 0.5}
	if (highwayType in highwayScores):
		score = highwayScores[highwayType]
	elif (highwayType == 'pedestrian'):
		if ('bicycle' in tags and (tags['bicycle'] == 'yes' or tags['bicycle'] == 'designated')):
			score = 0
		else:
			score = 0.5
	elif (highwayType == 'path' or highwayType == 'footway' or highwayType == 'track'):
		if (('surface' in tags and tags['surface'] in paved) or ('bicycle' in tags and (tags['bicycle'] == 'yes' or tags['bicycle'] == 'designated'))):
			score = 0
		else:
			score = 1.5
	elif (highwayType == 'cycleway'):
		if ('primaryside' in tags):
			score = highwayScores['primary'] - 2
		elif ('secondaryside' in tags):
			score = highwayScores['secondary'] - 2
		elif ('tertiaryside' in tags):
			score = highwayScores['tertiary'] - 2
		else:
			score = 0
	else:
		score = 1
				
	if ('cycleway' in tags and not (highwayType == 'cycleway')):
		cycleWayType = tags['cycleway']
		if (cycleWayType == 'lane' or cycleWayType == 'opposite_lane'):
			score -= 1
		elif (cycleWayType == 'track' or cycleWayType == 'opposite_track'):
			score = 0
			
	score = max(score, 0)

	if ('waterside' in tags):
		score -= 1
	elif ('treeland' in tags):
		score -= 1
	if ('bikeways' in tags) or ('mtbways' in tags):
		score -= 1
		
	return math.pow(4, score/4)
		
def getConstantScoreFast(tags):
	return 0

def getLinearScoreFast(tags):
	return 1
	
# Returns 0 for bidirectional traffic, 1 for traffic in the way direction, -1 for traffic in the opposite direction, and none if no cycling traffic is possible
def getBicycleDirection(tags):
	if ('bicycle' in tags and tags['bicycle'] == 'no'):
		return None
	unaccessible = {'raceway', 'motorway', 'motorway_link', 'bus_guideway', 'bridleway', 'steps', 'trunk', 'trunk_link'}
	highwayType = tags['highway']
	if (highwayType in unaccessible):
		return None
	if (highwayType == 'track'):
		# Tracks are only possible for bikes if they are paved or if cycling traffic is explicitely allowed
		if (not (('surface' in tags and tags['surface'] in paved) or ('bicycle' in tags and (tags['bicycle'] == 'yes' or tags['bicycle'] == 'designated')))):
			return None
	# TODO: Parse tags to determine cycling directions 0, 1 or -1
	return 0

class OsmScoreAdder(XMLFilterBase):
	def __init__(self, scoreinfos, parent=None):
		super().__init__(parent)
		self.scoreinfos = scoreinfos
		self.tags = dict()

	def startElement(self, name, attrs):
		if name=='way' or name == 'node':
			self.tags = dict()
		elif name=='tag':
			self.tags[attrs['k']] = attrs['v']
		super().startElement(name, attrs)

	def endElement(self, name):
		if name=='way':
			direction = getBicycleDirection(self.tags)
			if direction != None:
				super().startElement('tag', {'k': 'bicycle_oneway', 'v': str(direction)})
				super().endElement('tag')
				for scoreinfo in self.scoreinfos:
					length = float(self.tags['length'])
					score = max(0.1, scoreinfo[1](self.tags)+length*scoreinfo[2](self.tags))
					super().startElement('tag', {'k': scoreinfo[0], 'v': str(score)})
					super().endElement('tag')
					if length != 0:
						super().startElement('tag', {'k': scoreinfo[0] + '_norm', 'v': str(score/length)})
						super().endElement('tag')
		super().endElement(name)


def assign_cycling_scores(infile, outfile):
	scoreinfos = {('score_safe', getConstantScoreSafe, getLinearScoreSafe),
				  ('score_attr', getConstantScoreAttr, getLinearScoreAttr),
				  ('score_fast', getConstantScoreFast, getLinearScoreFast)}
	reader = OsmScoreAdder(scoreinfos, make_parser())
	with open(outfile, 'w', encoding='utf-8') as f:
		handler = XMLGenerator(f)
		reader.setContentHandler(handler)
		reader.parse(infile)

assign_cycling_scores(
	infile=os.path.join('..', 'graph', 'belgium_unweighted.osm'),
	outfile=os.path.join('..', 'graph', 'belgium_weighted.osm'))
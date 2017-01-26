from profile import Profile
import math

class BikeProfile(Profile):
	def getConstantScoreSafe(self, tags):
		score = 0
		if ('highway_primary' in tags):
			score += 200 * int(tags['highway_primary'])
		if ('highway_secondary' in tags):
			score += 100 * int(tags['highway_secondary'])
		if ('highway_tertiary' in tags):
			score += 50 * int(tags['highway_tertiary'])
		return score

	def getLinearScoreSafe(self, tags):
		if ('bicycle' in tags and tags['bicycle'] == 'no'):
			return -float('inf')

		score = 0
		unaccessible = ['raceway', 'motorway', 'motorway_link', 'bus_guideway', 'bridleway', 'steps', 'trunk', 'trunk_link']
		paved  = ['paved', 'asphalt', 'cobblestone', 'sett', 'concrete', 'concrete:lanes', 'concrete:plates', 'paving_stones', 'metal', 'gravel']
		highwayScores = {'primary': -4, 'primary_link': -4, 'secondary': -3, 'secondary_link': -3, 'tertiary': -2, 'tertiary_link': -2, 'unclassified': -1, 'service': -1, 'residential': -1, 'living_street': -0.7}
		highwayType = tags['highway']
		if (highwayType in unaccessible):
			return -float('inf')
		elif (highwayType in highwayScores):
			score += highwayScores[highwayType]
		elif (highwayType == 'pedestrian'):
			if ('bicycle' in tags and tags['bicycle'] == 'yes'):
				score = -0.5
			else:
				score = -0.7
		elif (highwayType == 'track'):
			if (('surface' in tags and tags['surface'] in paved) or ('bicycle' in tags and (tags['bicycle'] == 'yes' or tags['bicycle'] == 'designated'))):
				score = -0.5
			else:
				return -float('inf')
		elif (highwayType == 'path' or highwayType == 'footway'):
			if (('surface' in tags and tags['surface'] in paved) or ('bicycle' in tags and (tags['bicycle'] == 'yes' or tags['bicycle'] == 'designated'))):
				score = -0.5
			else:
				score = -3
		elif (highwayType == 'cycleway'):
				score = -0.5
				
		if ('cycleway' in tags and not (highwayType == 'cycleway')):
			cycleWayType = tags['cycleway']
			if (cycleWayType == 'lane' or cycleWayType == 'opposite_lane'):
				score += 1
			elif (cycleWayType == 'track' or cycleWayType == 'opposite_track'):
				score = -0.5
		
		return max(-score, 0.5)
		
	def getConstantScoreAttr(self, tags):
		pauze = ['bar', 'bbq', 'biergarten', 'cafe', 'drinking_water', 'fast_food', 'ice_cream', 'pub', 'restaurant', 'toilets']
		pauzeCnt = 0
		conf = ['bicycle_parking']
		confCnt = 0
		occ = ['bicycle_repair_station', 'bicycle_rental']
		occCnt = 0
		sight = ['fountain', 'place_of_worship']
		sightCnt = 0
		for tag in tags:
			if (tag in pauze):
				pauzeCnt += 1
			if (tag in conf):
				confCnt += 1
			if (tag in occ):
				occCnt += 1
			if (tag in sight):
				sightCnt += 1
		score = -100*sightCnt
		if (pauzeCnt):
			score -= 250
		if (confCnt):
			score -= 100
		if (occCnt):
			score -= 50
		return score

	def getLinearScoreAttr(self, tags):
		if ('bicycle' in tags and tags['bicycle'] == 'no'):
			return -float('inf')

		score = 0
		paved  = ['paved', 'asphalt', 'cobblestone', 'sett', 'concrete', 'concrete:lanes', 'concrete:plates', 'paving_stones', 'metal', 'gravel']
		highwayType = tags['highway']
		highwayScores = {'primary': -4, 'primary_link': -4, 'secondary': -3, 'secondary_link': -3, 'tertiary': -2, 'tertiary_link': -2, 'unclassified': 0, 'service': -1, 'residential': -1, 'living_street': -0.5}
		if (highwayType in highwayScores):
			score += highwayScores[highwayType]
		elif (highwayType == 'pedestrian'):
			if ('bicycle' in tags and tags['bicycle'] == 'yes'):
				score += 2
			else:
				score += 1
		elif (highwayType == 'track'):
			if (('surface' in tags and tags['surface'] in paved) or ('bicycle' in tags and (tags['bicycle'] == 'yes' or tags['bicycle'] == 'designated'))):
				score += 2
		elif (highwayType == 'path' or highwayType == 'footway'):
			if (('surface' in tags and tags['surface'] in paved) or ('bicycle' in tags and (tags['bicycle'] == 'yes' or tags['bicycle'] == 'designated'))):
				score += 2
		elif (highwayType == 'cycleway'):
			if ('primaryside' in tags):
				score += highwayScores['primary'] + 1
			elif ('secondaryside' in tags):
				score += highwayScores['secondary'] + 1
			elif ('tertiaryside' in tags):
				score += highwayScores['tertiary'] + 1
				
		if ('cycleway' in tags and not (highwayType == 'cycleway')):
			cycleWayType = tags['cycleway']
			if (cycleWayType == 'lane' or cycleWayType == 'opposite_lane'):
				score += 1
			elif (cycleWayType == 'track' or cycleWayType == 'opposite_track'):
				score += 3

		if ('waterside' in tags):
			score += 1
		elif ('forest' in tags):
			score += 1
		if ('bikeways' in tags):
			nrBikeways = int(tags['bikeways'])
			if (nrBikeways == 1):
				score += 1
			else:
				score += 1.5
		
		return math.pow(4, -score/4)
		
	def getConstantScoreFast(self, tags):
		return 0

	def getLinearScoreFast(self, tags):
		return 1
		
	def addTags(self, wayAttrs, tags):
		wayQualityScoreSafe = self.getLinearScoreSafe(tags)
		POIscoreSafe = self.getConstantScoreSafe(tags)
		wayQualityScoreAttr = self.getLinearScoreAttr(tags)
		POIscoreAttr = self.getConstantScoreAttr(tags)
		wayQualityScoreFast = self.getLinearScoreFast(tags)
		POIscoreFast = self.getConstantScoreFast(tags)
		try:
			length = float(tags['length'])
			if length == 0:
				wayQualityScoreSafe = 0
				wayQualityScoreAttr = 0
				wayQualityScoreFast = 0
			scoreSafe = wayQualityScoreSafe*length + POIscoreSafe
			scoreAttr = wayQualityScoreAttr*length + POIscoreAttr
			scoreFast = wayQualityScoreFast*length + POIscoreFast
		except (ValueError, KeyError):
			scoreSafe = float('inf')
			scoreAttr = float('inf')
			scoreFast = float('inf')
			
		if (not math.isinf(scoreSafe)) and (not math.isinf(scoreAttr)) and (not math.isinf(wayQualityScoreFast)):
			wayAttrs['allows_bikes'] = '1'
			tags['score_safe_lin'] = wayQualityScoreSafe
			tags['score_attr_lin'] = wayQualityScoreAttr
			tags['score_fast_lin'] = wayQualityScoreFast
			tags['score_safe_const'] = POIscoreSafe
			tags['score_attr_const'] = POIscoreAttr
			tags['score_fast_const'] = POIscoreFast
		else:
			wayAttrs['allows_bikes'] = '0'
		# Add direction
		oneway_tag = None
		if ('oneway:bicycle' in tags):
			oneway_tag = 'oneway:bicycle'
		elif ('oneway' in tags) and not ('cycleway' in tags and (tags['cycleway'] in ['opposite', 'opposite_lane', 'opposite_track'])):
			oneway_tag = 'oneway'
		
		if oneway_tag and (tags[oneway_tag] == 'yes' or tags[oneway_tag] == '1'):
			tags['bicycle_oneway'] = '1'
		elif oneway_tag and tags[oneway_tag] == '-1':
			tags['bicycle_oneway'] = '-1'
		else:
			tags['bicycle_oneway'] = '0'
		return tags
		
	@classmethod
	def getName(cls):
		return 'bikeProfile'
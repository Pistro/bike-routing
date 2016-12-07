from base.core import InterReader
from reader.osm.wayWayNode import WayWayNode
from reader.osm.nodeLatLon import NodeLatLon
from reader.inter.wayMapper import WayMapper
import xml.sax, xml.sax.saxutils, time
from haversine import haversine

class PassNearReader(InterReader):
	def __init__(self, opts):
		self.share = opts['share']
		self.id = opts['id']
		self.inPath = opts['in']
		self.tag = opts['tag']
		self.searchDist = 25
		if ('dist' in opts):
			self.searchDist = float(opts['dist'])
		self.maxSearchArea = 40000
		if ('maxArea' in opts):
			self.searchDist = float(opts['maxArea'])
			
	def updateBounds(self, bounds, newNode):
		if newNode[0]<bounds[0]:
			bounds[0] = newNode[0]
		if newNode[1]<bounds[1]:
			bounds[1] = newNode[1]
		if newNode[0]>bounds[2]:
			bounds[2] = newNode[0]
		if newNode[1]>bounds[3]:
			bounds[3] = newNode[1]
						
	def execute(self):
		(waterNodes, waterWays, waterMap) = self.getWaterInfo()
		(wayNodes, ways, wayMap) = (self.nodes, self.ways, self.wm)
		self.wayAttr = dict()
		researched = set()
		for waterWayId in waterWays:
			waterWay = waterWays[waterWayId]
			waterWay.append(0)
			mPerLat = None
			mPerLon = None
			segments = list()
			bounds = [float('inf'), float('inf'), -float('inf'), -float('inf')]
			for i in range(len(waterWay)):
				if waterWay[i] in waterNodes:
					node = waterNodes[waterWay[i]]
					if not mPerLat:
						(lat, lon) = node
						mPerLat = haversine((lat-0.5, lon), (lat+0.5, lon))*1000
						mPerLon = haversine((lat, lon-0.5), (lat, lon+0.5))*1000
					self.updateBounds(bounds, node)
					newArea = ((bounds[2]-bounds[0])*mPerLat+2*self.searchDist)*((bounds[3]-bounds[1])*mPerLon+2*self.searchDist)
					if (waterWay[i] in waterNodes and newArea>self.maxSearchArea and len(segments)>1):
						self.researchWater(segments, researched, ways, wayNodes, wayMap, waterMap)
						lastNode = segments[len(segments)-1]
						segments = [lastNode]
						bounds = [lastNode[0], lastNode[1], lastNode[0], lastNode[1]]
					self.updateBounds(bounds, node)
					area = ((bounds[2]-bounds[0])*mPerLat+2*self.searchDist)*((bounds[3]-bounds[1])*mPerLon+2*self.searchDist)
					segments.append(node)
				elif len(segments):
					self.researchWater(segments, researched, ways, wayNodes, wayMap, waterMap)
					segments = list()
					bounds = [float('inf'), float('inf'), -float('inf'), -float('inf')]
				
	def researchWater(self, segments, researched, ways, wayNodes, wayMap, waterMap):
		candidates = wayMap.getCloseWays(segments, self.searchDist)
		for candidateId in candidates:
			if not candidateId in researched:
				researched.add(candidateId)
				candidateWay = ways[candidateId]
				candidateStart = wayNodes[candidateWay[0]]
				candidateStop = wayNodes[candidateWay[len(candidateWay)-1]]
				if waterMap.hasCloseWay([candidateStart], [self.searchDist]) and waterMap.hasCloseWay([candidateStop], [self.searchDist]):
					self.wayAttr[candidateId] = {self.tag: 1}
	
	def getWaterInfo(self):
		wwn = WayWayNode({'share': 0, 'id': 0})
		nll = NodeLatLon({'share': 0, 'id': 0})
		nll.setWriter(wwn.setWriter(xml.sax.ContentHandler()))
		xml.sax.parse(self.inPath, nll)
		wm = WayMapper({'share': 0, 'id': 0})
		wm.ways = wwn.ways
		wm.nodes = nll.nodes
		wm.execute()
		return (nll.nodes, wwn.ways, wm)
		
	def imports(self):
		return {'wayMapper_' + self.share: 'wm', 'dict-way-nodes_' + self.share: 'ways', 'dict-node-latLon_' + self.share: 'nodes'}
		
	def exports(self):
		return {'dict-way-wayAttr_' + self.id: 'wayAttr'}
		
	@staticmethod
	def shortName():
		return 'passNea-wWAtt'
		
	@staticmethod
	def longName():
		return 'passNear-wWAtt'
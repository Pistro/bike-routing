from base.core import InterReader
from base.tagRemapper import TagRemapper
import xml.sax, xml.sax.saxutils, time
import array
from base.largeDicts import CoordinateDict

class PassThroughRelReader(xml.sax.ContentHandler, InterReader):
	def __init__(self, opts):
		self.share = opts['share']
		self.id = opts['id']
		self.tagRemapper = TagRemapper(opts['remap'])
		self.inPath = opts['in']

		self.wayAttr = dict()
		
		self.nodes = CoordinateDict()
		self.ways = dict()
		self.inRelation = False
		
	def startElement(self, name, attrs):
		if name=='node':
			self.nodes[int(attrs['id'])] = (float(attrs['lat']), float(attrs['lon']))
		elif name== 'way':
			self.wayNodes = array.array('I')
			self.wayId = int(attrs['id'])
		elif (name == 'nd'):
			self.wayNodes.append(int(attrs['ref']))
		elif name== 'relation':
			self.outerWays = set()
			self.innerWays = set()
			self.keys = set()
			self.inRelation = True
		elif (name == 'member'):
			if (attrs['role'] == 'outer'):
				self.outerWays.add(int(attrs['ref']))
			else:
				self.innerWays.add(int(attrs['ref']))
		elif (name == 'tag' and self.inRelation):
			self.tagRemapper.addMatches(attrs, self.keys)
		
	def endElement(self, name):
		if (name == 'way'):
			self.ways[self.wayId] = self.wayNodes
		elif (name == 'relation'):
			outers = self.getWays(self.outerWays)
			if len(outers):
				inners = self.getWays(self.innerWays)
				intersectingWays = self.wm.getInnerWays(outers, inners)
				for way in intersectingWays:
					if not way in self.wayAttr:
						self.wayAttr[way] = dict()
					wayDict = self.wayAttr[way]
					for key in self.keys:
						if not key in wayDict:
							wayDict[key] = 0
						wayDict[key] += 1
			self.inRelation = False
	
	def getWays(self, wayIdList):
		out = list()
		for wayId in wayIdList:
			if wayId in self.ways:
				wayNodes = self.ways[wayId]
				points = list()
				for point in wayNodes:
					if (point in self.nodes):
						points.append(self.nodes[point])
				if (len(points)>2):
					out.append(points)
		return out
		
	def execute(self):
		xml.sax.parse(self.inPath, self)
		del self.nodes
		del self.ways
		del self.wayNodes

	def imports(self):
		return {'wayMapper_' + self.share: 'wm'}
		
	def exports(self):
		return {'dict-way-wayAttr_' + self.id: 'wayAttr'}
		
	@staticmethod
	def shortName():
		return 'passThrRel-wWAtt'

	@staticmethod
	def longName():
		return 'passThroughRel-wWAtt'
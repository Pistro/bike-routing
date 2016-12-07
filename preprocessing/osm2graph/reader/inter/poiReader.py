from base.core import InterReader
from base.tagRemapper import TagRemapper
import xml.sax, xml.sax.saxutils, time
from base.largeDicts import CoordinateDict

class PoiReader(xml.sax.ContentHandler, InterReader):
	def __init__(self, opts):
		self.share = opts['share']
		self.id = opts['id']
		self.tagRemapper = TagRemapper(opts['remap'])

		self.wayAttr = dict()                      # wayId -> {'attr1': count(attr1), 'attr2': count(attr2), ...}
		self.nodes = CoordinateDict()
		self.inPath = opts['in']
		self.searchDist = [15, 35, 100]
		if ('dist' in opts):
			self.searchDist = readFlList(opts['dist'])
		self.way = None
			
	def startElement(self, name, attrs):
		if name=='node':
			self.nodes[int(attrs['id'])] = (float(attrs['lat']), float(attrs['lon']))
			self.nodeId = int(attrs['id'])
			self.keys = set()
		elif name=='way':
			self.wayNodes = list()
			self.keys = set()
		elif (name == 'nd'):
			self.wayNodes.append(int(attrs['ref']))
		elif (name == 'tag'):
			self.tagRemapper.addMatches(attrs, self.keys)
		
	def endElement(self, name):
		if (name == 'way'):
			points = list()
			for point in self.wayNodes:
				if (point in self.nodes):
					points.append(self.nodes[point])
			if (len(points)>2):
				self.way = self.wm.getClosestWay(points, self.searchDist)
		elif (name == 'node'and len(self.keys)):
				self.way = self.wm.getClosestWay([self.nodes[self.nodeId]], self.searchDist)
		if (self.way!=None):
			if not self.way in self.wayAttr:
				self.wayAttr[self.way] = dict()
			wayDict = self.wayAttr[self.way]
			for key in self.keys:
				if not key in wayDict:
					wayDict[key] = 0
				wayDict[key] += 1
			self.way = None
			
	def execute(self):
		xml.sax.parse(self.inPath, self)
		del self.nodes
		del self.wayNodes

	def imports(self):
		return {'wayMapper_' + self.share: 'wm'}
		
	def exports(self):
		return {'dict-way-wayAttr_' + self.id: 'wayAttr'}

	@staticmethod
	def shortName():
		return 'poi-wWAtt'
		
	@staticmethod
	def longName():
		return 'poi-wWAtt'
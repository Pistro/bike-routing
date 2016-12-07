from base.core import InterReader
from base.tagRemapper import TagRemapper
import xml.sax, xml.sax.saxutils, time
import array
from base.largeDicts import CoordinateDict

class PassThroughWayReader(xml.sax.ContentHandler, InterReader):
	def __init__(self, opts):
		self.share = opts['share']
		self.id = opts['id']
		self.tagRemapper = TagRemapper(opts['remap'])

		self.wayAttr = dict()                      # wayId -> {'attr1': count(attr1), 'attr2': count(attr2), ...}
		self.nodes = CoordinateDict()
		self.inPath = opts['in']
		self.inWay = False
			
	def startElement(self, name, attrs):
		if name=='node':
			self.nodes[int(attrs['id'])] = (float(attrs['lat']), float(attrs['lon']))
		elif name=='way':
			self.wayNodes = array.array('I')
			self.keys = set()
			self.inWay = True
		elif (name == 'nd'):
			self.wayNodes.append(int(attrs['ref']))
		elif (name == 'tag' and self.inWay):
			self.tagRemapper.addMatches(attrs, self.keys)
		
	def endElement(self, name):
		if (name == 'way'):
			points = list()
			for point in self.wayNodes:
				if (point in self.nodes):
					points.append(self.nodes[point])
			if (len(points)>2):
				intersectingWays = self.wm.getInnerWays([points])
				for way in intersectingWays:
					if not way in self.wayAttr:
						self.wayAttr[way] = dict()
					wayDict = self.wayAttr[way]
					for key in self.keys:
						if not key in wayDict:
							wayDict[key] = 0
						wayDict[key] += 1
			
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
		return 'passThrWay-wWAtt'

	@staticmethod
	def longName():
		return 'passThroughWay-wWAtt'
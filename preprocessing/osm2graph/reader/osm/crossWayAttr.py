from base.core import OsmReader
from base.tagRemapper import TagRemapper

class CrossWayAttr(OsmReader):
	def __init__(self, opts):
		self.share = opts['share']
		self.nodes = dict() # node -> {wayAttr1: cnt(wayAttr1), wayAttr2: cnt(wayAttr2)}
		self.tagRemapper = TagRemapper(opts['remap'])
		self.endings = 'endings' in opts
		self.wayAttrs = None

	def startElement(self, name, attrs):
		if name=='way':
			self.wayNodes = list()
			self.tags = set()
			self.wayAttrs = dict(attrs)
		elif (self.wayAttrs != None):
			if (name == 'nd'):
				self.wayNodes.append(int(attrs['ref']))
			elif (name == 'tag'):
				self.tagRemapper.addMatches(attrs, self.tags)
		self.writer.startElement(name, attrs)
		
	def endElement(self, name):
		if name=='way':
			if self.endings and len(self.wayNodes):
				self.wayNodes = [self.wayNodes[0], self.wayNodes[len(self.wayNodes)-1]]
			for nodeId in self.wayNodes:
				if not nodeId in self.nodes:
					self.nodes[nodeId] = dict()
				nodeDict = self.nodes[nodeId]					
				for key in self.tags:
					if not key in nodeDict:
						nodeDict[key] = 0
					nodeDict[key] += 1
			self.wayAttrs = None
		self.writer.endElement(name)

	def exports(self):	
		return {'dict-node-wayAttr_' + self.share: 'nodes'}

	@staticmethod
	def shortName():
		return 'nWAtt'

	@staticmethod
	def longName():
		return 'nodeWayAtt'
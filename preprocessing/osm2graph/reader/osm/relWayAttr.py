from base.core import OsmReader
from base.tagRemapper import TagRemapper

class RelWayAttr(OsmReader):
	def __init__(self, opts):
		self.id = opts['id']
		self.tagRemapper = TagRemapper(opts['remap'])
		
		self.wayAttr = dict() # wayId -> {'attr1': count(attr1), 'attr2': count(attr2), ...}
		self.relation = False
	
	def startElement(self, name, attrs):
		if (name == 'relation'):
			self.wSet = set()
			self.wKeys = set()
			self.relation = True
		if self.relation:
			if (name == 'member' and attrs['type']=='way'):
				self.wSet.add(attrs['ref'])
			elif (name == 'tag'):
				self.tagRemapper.addMatches(attrs, self.wKeys)
		self.writer.startElement(name, attrs)
				
	def endElement(self,name):
		if (name == 'relation'):
			for way in self.wSet:
				if not way in self.wayAttr:
					self.wayAttr[way] = dict()
				wayDict = self.wayAttr[way]
				for key in self.wKeys:
					if not key in wayDict:
						wayDict[key] = 0
					wayDict[key] += 1
			self.relation = False
		self.writer.endElement(name)

	def exports(self):
		return {'dict-way-wayAttr_' + self.id: 'wayAttr'}
		
	@staticmethod
	def shortName():
		return 'r-wWAtt'

	@staticmethod
	def longName():
		return 'rel-wayWayAtt'
from base.core import OsmReader

class NodeCounter(OsmReader):
	def __init__(self, opts):
		self.share = opts['share']
		self.unrefNodes = set()
		self.inbetweenNodes = set()
		self.crossings = set()
			
	def startElement(self, name, attrs):
		if name=='node':
			self.unrefNodes.add(int(attrs['id']))
		elif (name == 'nd'):
			id = int(attrs['ref'])
			if (not id in self.crossings):
				if (id in self.inbetweenNodes):
					self.inbetweenNodes.remove(id)
					self.crossings.add(id)
				elif (id in self.unrefNodes):
					self.inbetweenNodes.add(id)
					self.unrefNodes.remove(id)
		self.writer.startElement(name, attrs)

	def endElement(self, name):
		self.writer.endElement(name)

	def exports(self):
		return {'set-crossings_' + self.share: 'crossings', 'set-inbetweenNodes_' + self.share: 'inbetweenNodes'}
		
	@staticmethod
	def shortName():
		return 'nCnt'

	@staticmethod
	def longName():
		return 'nodeCounter'
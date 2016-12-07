from base.core import OsmReader
import array

class WayWayNode(OsmReader):
	def __init__(self, opts):
		self.share = opts['share']
		self.ways = dict()

	def startElement(self, name, attrs):
		if name=='way':
			self.wayId = attrs['id']
			self.wayNodes = array.array('I')
		elif (name == 'nd'):
			self.wayNodes.append(int(attrs['ref']))
		self.writer.startElement(name, attrs)
		
	def endElement(self, name):
		if name=='way':
			self.ways[self.wayId] = self.wayNodes
		self.writer.endElement(name)

	def exports(self):	
		return {'dict-way-nodes_' + self.share: 'ways'}

	@staticmethod
	def shortName():
		return 'wNde'

	@staticmethod
	def longName():
		return 'wayNode'
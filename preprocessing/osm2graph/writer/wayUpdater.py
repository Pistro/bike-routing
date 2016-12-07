from base.core import OsmWriter

class WayUpdater(OsmWriter):
	def __init__(self, opts):
		self.share = opts['share']
		self.id = opts['id']
		self.attrs = None

	def startElement(self, name, attrs):
		if name=='way':
			self.nodes = list()
			self.tags = dict()
			self.attrs = dict(attrs)
		elif (self.attrs != None):
			if (name == 'nd'):
				self.nodes.append(int(attrs['ref']))
			elif (name == 'tag'):
				self.tags[attrs['k']] = attrs['v']
		else:
			self.writer.startElement(name, attrs)
		
	def endElement(self, name):
		if name=='way':
			(self.attrs, self.tags) = self.updateWay(self.attrs, self.tags, self.nodes)
			self.writer.startElement('way', self.attrs)
			for nd in self.nodes:
				self.writer.startElement('nd', {'ref': str(nd)})
				self.writer.endElement('nd')
			for tag in self.tags:
				self.writer.startElement('tag', {'k': tag, 'v': str(self.tags[tag])})
				self.writer.endElement('tag')
			self.writer.endElement('way')
			
			self.attrs = None
		elif (not self.attrs):
			self.writer.endElement(name)
			
	def updateWay(attrs, tags, nodes):
		raise NotImplementedError('Each wayupdater should either override the updateWay or the endElement function')
		
	@staticmethod
	def shortName():
		return ' '

	@staticmethod
	def longName():
		return ' '

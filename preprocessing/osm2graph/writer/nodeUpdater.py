from base.core import OsmWriter

class NodeUpdater(OsmWriter):
	def __init__(self, opts):
		self.share = opts['share']
		self.id = opts['id']
		self.attrs = None

	def startElement(self, name, attrs):
		if name=='node':
			self.tags = dict()
			self.attrs = dict(attrs)
		elif (self.attrs != None):
			if (name == 'tag'):
				self.tags[attrs['k']] = attrs['v']
		else:
			self.writer.startElement(name, attrs)
		
	def endElement(self, name):
		if name=='node':
			(self.attrs, self.tags) = self.updateNode(self.attrs, self.tags)
			self.writer.startElement('node', self.attrs)
			for tag in self.tags:
				self.writer.startElement('tag', {'k': tag, 'v': str(self.tags[tag])})
				self.writer.endElement('tag')
			self.writer.endElement('node')
			
			self.attrs = None
		elif (not self.attrs):
			self.writer.endElement(name)
			
	def updateNode(attrs, tags):
		raise NotImplementedError('Each wayupdater should either override the updateNode or the endElement function')
		
	@staticmethod
	def shortName():
		return ' '

	@staticmethod
	def longName():
		return ' '

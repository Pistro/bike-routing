from writer.wayUpdater import WayUpdater

class WayCleaner(WayUpdater):
	def __init__(self, opts):
		super().__init__(opts)

	def endElement(self, name):
		if name=='way':
			valid = True
			for node in self.nodes:
				if (not node in self.crossings) and (not node in self.inbetweenNodes):
					valid = False
			if (valid):
				self.writer.startElement('way', self.attrs)
				for node in self.nodes:
					self.writer.startElement('nd', {'ref': str(node)})
					self.writer.endElement('nd')
				for tag in self.tags:
					self.writer.startElement('tag', {'k': tag, 'v': self.tags[tag]})
					self.writer.endElement('tag')
				self.writer.endElement('way')
			self.attrs = None
		elif (not self.attrs):
			self.writer.endElement(name)
			
	def imports(self):
		return {'set-crossings_' + self.share: 'crossings', 'set-inbetweenNodes_' + self.share: 'inbetweenNodes'}
		
	@staticmethod
	def shortName():
		return 'wCl'

	@staticmethod
	def longName():
		return 'wayCleaner'
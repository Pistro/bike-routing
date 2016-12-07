from base.core import OsmWriter

class NodeCleaner(OsmWriter):
	def __init__(self, opts):
		super().__init__(opts)
		self.write = True;

	def startElement(self, name, attrs):
		if name=='node':
			attrs = dict(attrs)
			if (int(attrs['id']) in self.inbetweenNodes or int(attrs['id']) in self.crossings):
				self.writer.startElement('node', attrs)
				self.write = True
			else:
				self.write = False
		elif (self.write):
			self.writer.startElement(name, attrs)
			
	def endElement(self, name):
		if (self.write):
			self.writer.endElement(name)
		if name=='node':
			self.write = True

	def imports(self):
		return {'set-crossings_' + self.share: 'crossings', 'set-inbetweenNodes_' + self.share: 'inbetweenNodes'}
		
	@staticmethod
	def shortName():
		return 'nr'

	@staticmethod
	def longName():
		return 'nodeRemover'
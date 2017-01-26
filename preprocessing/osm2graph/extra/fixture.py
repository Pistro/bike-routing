import xml.sax, xml.sax.saxutils

from base.core import OsmWriter
from base.core import ComboOsmProcessor
from writer.styledXMLWriter import StyledXMLWriter

class FixtureWriter(OsmWriter):
	def __init__(self, opts):
		self.xmlWriter = StyledXMLWriter(opts)
		self.xmlWriter.setWriter(xml.sax.ContentHandler())
		
	def startDocument(self):
		self.xmlWriter.startDocument()
		self.xmlWriter.startElement("django-objects", {"version": "1.0"})
		self.writer.startDocument()
		self.tags = None
		self.storedTags = dict()

	def endDocument(self):
		self.xmlWriter.endElement("django-objects")
		self.xmlWriter.endDocument()
		self.writer.endDocument()
		
	def startElement(self, name, attrs):
		if (name == 'node'):
			self.attrs = attrs
			self.tags = dict()
		elif (name == 'way'):
			self.attrs = attrs
			self.nodes = list()
			self.tags = dict()
		elif (name == 'nd' and (self.tags!=None)):
			self.nodes.append(attrs['ref'])
		elif (name == 'tag' and (self.tags!=None)):
			self.tags[attrs['k']] = attrs['v']
		self.writer.startElement(name, attrs)
		
	def endElement(self, name):
		if (name == 'way'):
			for key in self.tags:
				if (not (key == 'original_id' or key=='original_subid')) and (not (key, self.tags[key]) in self.storedTags):
					self.storedTags[(key, self.tags[key])] = len(self.storedTags)
					self.xmlWriter.startElement('object', {'pk': str(self.storedTags[(key, self.tags[key])]), 'model': 'showRoutes.tag'})
					self.xmlWriter.startElement('field', {'type': 'TextField', 'name': 'key'})
					self.xmlWriter.outFile.write(key)
					self.xmlWriter.endElement('field')
					self.xmlWriter.startElement('field', {'type': 'TextField', 'name': 'value'})
					self.xmlWriter.outFile.write(self.tags[key].replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))
					self.xmlWriter.endElement('field')
					self.xmlWriter.endElement('object')
			self.xmlWriter.startElement('object', {'pk': str(self.attrs['id']), 'model': 'showRoutes.way'})
			self.xmlWriter.startElement('field', {'type': 'IntegerField', 'name': 'orgWayId'})
			self.xmlWriter.outFile.write(str(self.tags['original_id']))
			self.xmlWriter.endElement('field')
			self.xmlWriter.startElement('field', {'type': 'IntegerField', 'name': 'orgWaySubId'})
			self.xmlWriter.outFile.write(str(self.tags['original_subid']))
			self.xmlWriter.endElement('field')
			self.xmlWriter.startElement('field', {'type': 'ManyToManyField', 'name': 'tags'})
			for key in self.tags:
				if (not (key == 'original_id' or key=='original_subid')):
					self.xmlWriter.startElement('object', {'pk': str(self.storedTags[(key, self.tags[key])])})
					self.xmlWriter.endElement('object')
			self.xmlWriter.endElement('field')
			self.xmlWriter.endElement('object')
			for nodeIndex in range(len(self.nodes)):
				self.xmlWriter.startElement('object', {'model': 'showRoutes.membership'})
				self.xmlWriter.startElement('field', {'type': 'IntegerField', 'name': 'way'})
				self.xmlWriter.outFile.write(self.attrs['id'])
				self.xmlWriter.endElement('field')
				self.xmlWriter.startElement('field', {'type': 'IntegerField', 'name': 'node'})
				self.xmlWriter.outFile.write(self.nodes[nodeIndex])
				self.xmlWriter.endElement('field')
				self.xmlWriter.startElement('field', {'type': 'IntegerField', 'name': 'pos'})
				self.xmlWriter.outFile.write(str(nodeIndex))
				self.xmlWriter.endElement('field')
				self.xmlWriter.endElement('object')
			self.tags = None
		elif (name == 'node'):
			self.xmlWriter.startElement('object', {'pk': str(self.attrs['id']), 'model': 'showRoutes.node'})
			self.xmlWriter.startElement('field', {'type': 'FloatField', 'name': 'lat'})
			self.xmlWriter.outFile.write(str(self.attrs['lat']))
			self.xmlWriter.endElement('field')
			self.xmlWriter.startElement('field', {'type': 'FloatField', 'name': 'lon'})
			self.xmlWriter.outFile.write(str(self.attrs['lon']))
			self.xmlWriter.endElement('field')
			self.xmlWriter.endElement('object')
			self.tags = None
		self.writer.endElement(name)

	@staticmethod
	def shortName():
		return 'w-fix'

	@staticmethod
	def longName():
		return 'w-fix'
	
	
class WriteFixture(ComboOsmProcessor):
	def __init__(self, opts):
		self.writer = FixtureWriter(opts)
		
	def shedule(self, opts, shedule, step):
		self.writer.shedule({"life": str(step) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'fix'

	@classmethod
	def longName(cls):
		return 'fix'
		
	@classmethod
	def getInfo(cls):
		return ("[Writer] Write the output to a Django fixture xml-file.", [("out","The file to which the output is writen.")])
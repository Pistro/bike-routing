import xml.sax, xml.sax.saxutils
from base.core import OsmWriter

class StyledXMLWriter(OsmWriter):
	def __init__(self, opts):
		self.outPath = opts['out']
		self.lastName = None
		self.indentation = 0
				
	def startDocument(self):
		self.outFile = open(self.outPath, 'w', encoding='utf-8')
		self.wr = xml.sax.saxutils.XMLGenerator(self.outFile, 'UTF-8')
		self.wr.startDocument()
		self.writer.startDocument()
				
	def endDocument(self):
		self.wr.endDocument()
		self.outFile.close()
		self.writer.endDocument()

	def startElement(self, name, attrs):
		if (self.lastName != None):
			self.outFile.write("\n")
		for i in range(0, self.indentation):
			self.outFile.write(" ")
		self.indentation += 2
		self.lastName = name
		self.wr.startElement(name, attrs)
		self.writer.startElement(name, attrs)
				
	def endElement(self,name):
		self.indentation -= 2
		if (name != self.lastName):
			self.outFile.write("\n")
			for i in range(0, self.indentation):
				self.outFile.write(" ")
		self.lastName = name
		self.wr.endElement(name)
		self.writer.endElement(name)
		
	@staticmethod
	def shortName():
		return 'w-xml'

	@staticmethod
	def longName():
		return 'write-xml'
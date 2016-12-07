from base.core import ComboOsmProcessor
from writer.styledXMLWriter import StyledXMLWriter

class WriteXml(ComboOsmProcessor):
	def __init__(self, opts):
		self.writer = StyledXMLWriter(opts)
		
	def shedule(self, opts, shedule, step):
		self.writer.shedule({"life": str(step) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'xml'
		
	@classmethod
	def longName(cls):
		return 'xml'
		
	@classmethod
	def getInfo(cls):
		return ("[Writer] Write the output to an XML-file.", [("out","The file to which the output is writen.")])
import xml.sax, xml.sax.saxutils
from .parse import *

class OsmProcessor:
	@classmethod
	def shortName(cls):
		raise NotImplementedError("Each OsmProcessor should override the 'shortname' function: " + cls.__name__)

	@classmethod
	def longName(cls):
		raise NotImplementedError("Each OsmProcessor should override the 'longName' function: " + cls.__name__)

class ComboOsmProcessor(OsmProcessor):			
	@classmethod
	def getInfo(cls):
		raise NotImplementedError("Each ComboOsmProcessor should override the 'getInfo' function: " + cls.__name__)
	
class SimpleOsmProcessor(OsmProcessor):
	@classmethod
	def imports(self):
		return dict()

	@classmethod
	def exports(self):
		return dict()

class RecursiveProcessor(xml.sax.ContentHandler):
	def setWriter(self, writer):
		self.writer = writer
		return self

	def startDocument(self):
		self.writer.startDocument()
				
	def endDocument(self):
		self.writer.endDocument()

	def startElement(self, name, attrs):
		self.writer.startElement(name, attrs)

	def endElement(self, name):
		self.writer.endElement(name)
		
class Reader(SimpleOsmProcessor):
	def shedule(self, opts, shedule):
		sheduleList = shedule['steps']
		(lifeStart, lifeStopStr) = readRange(opts['life'])
		if lifeStopStr == 'end':
			lifeStop = max(lifeStart+1, len(sheduleList))
		else:
			lifeStop = lifeStopStr
		while (len(sheduleList)<max(lifeStop, lifeStart+1)):
			sheduleList.append(deepCopySheduleStep(shedule['end']))
		exported = self.exports()
		addNecessary = False
		for name in exported:
			if (not name in sheduleList[lifeStart-1]['resources']) and (lifeStart<2 or (not name in sheduleList[lifeStart-2]['resources'])):
				addNecessary = True
		if addNecessary:
			self.addExecutor(lifeStart, shedule)
		for i in range(lifeStart-1, lifeStop):
			for name in exported:
				sheduleList[i]['resources'].add(name)
		if lifeStopStr == 'end':
			for name in exported:
				shedule['end']['resources'].add(name)

class OsmReader(RecursiveProcessor, Reader):
	def shedule(self, opts, shedule):
		Reader.shedule(self, opts, shedule)
		
	def addExecutor(self, lifeStart, shedule):
		shedule['steps'][lifeStart]['readers'].append(self)

class InterReader(Reader):
	def shedule(self, opts, shedule):
		Reader.shedule(self, opts, shedule)
		
	def addExecutor(self, lifeStart, shedule):
		shedule['steps'][lifeStart]['interreaders'].append(self)
		
class OsmWriter(SimpleOsmProcessor, RecursiveProcessor):
	def shedule(self, opts, shedule):
		sheduleList = shedule['steps']
		(lifeStart, lifeStopStr) = readRange(opts['life'])
		if lifeStopStr == 'end':
			lifeStop = max(lifeStart, len(sheduleList)-1)
		else:
			lifeStop = lifeStopStr
		while (len(sheduleList)<=lifeStop):
			sheduleList.append(deepCopySheduleStep(shedule['end']))
		exported = self.exports()
		for i in range(lifeStart, lifeStop+1):
			sheduleList[i]['readers'].append(self)
		if lifeStopStr == 'end':
			shedule['end']['readers'].append(self)
			
def deepCopySheduleStep(step):
	return {'readers': list(step['readers']), 'interreaders': list(step['interreaders']), 'resources': set(step['resources'])}
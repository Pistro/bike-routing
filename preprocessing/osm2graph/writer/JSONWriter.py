import xml.sax, xml.sax.saxutils
from base.core import OsmWriter
import json

class JSONWriter(OsmWriter):
	def __init__(self, opts):
		self.handleDict = {'bounds': 'attObj', 'node': 'attArrObj', 'way': 'attArrObj', 'relation': 'attArrObj', 'member': 'attArrObj', 'nd': 'attArr'}
		self.outPath = opts['out']
		
	def startDocument(self):
		self.outFile = open(self.outPath, 'w', encoding='utf-8')
		self.outFile.write("{")
		self.hierarchy = list()
		self.indent = 1
		self.writer.startDocument()

	def endDocument(self):
		self.handleArrObj(None)
		self.outFile.write("\n}")
		self.outFile.close()
		self.writer.endDocument()
		
	def handleArrObj(self, name):
		# We assume no nested arrays/shared objects
		if (len(self.hierarchy) and self.hierarchy[len(self.hierarchy)-1][0]=="_"):
			if not self.hierarchy[len(self.hierarchy)-1] == ("_" + str(name)):
				self.hierarchy.pop()
				self.outFile.write("\n")
				self.indent -= 1
				for i in range(self.indent):
					self.outFile.write(" ")
				self.outFile.write("]")
				return self.handleArrObj(name)
			else:
				self.outFile.write(",")
				return True
		return False
		
	def start(self, type, name, extra):
		# Finish up the previous lines
		if not self.handleArrObj(name):
			if (len(self.hierarchy) and self.hierarchy[len(self.hierarchy)-1] == 'att'):
				self.outFile.write(",")
			else:
				self.hierarchy.append("att")
		#Start new line
		self.outFile.write("\n")
		for i in range(self.indent):
			self.outFile.write(" ")
		# Fill in the new line
		if (type == "att"):
			self.outFile.write('"' + name.replace('"', "'") + '": "' + extra.replace('"', "'") + '"')
		elif (type == "attArrObj" or type == "attArr"):
			if not len(self.hierarchy) or not self.hierarchy[len(self.hierarchy)-1] == ("_" + name):
				self.outFile.write('"' + name + '": [\n')
				self.indent += 1
				self.hierarchy.append("_" + name)
				for i in range(self.indent):
					self.outFile.write(" ")
			self.indent += 1
			self.hierarchy.append(name)
			if (type == "attArrObj"):
				self.outFile.write("{")
				for attr in extra:
					self.start("att", attr, extra[attr])
			else:
				self.outFile.write('"' + extra + '"')
		elif (type == "attObj"):
			self.outFile.write('"' + name + '": {')
			self.indent += 1
			self.hierarchy.append(name)
			for attr in extra:
				self.start("att", attr, extra[attr])

	def startElement(self, name, attrs):
		attrs = dict(attrs)
		if (name == 'tag'):
			self.start("att", "tag_" + attrs['k'], attrs['v'])
		elif (name == 'nd'):
			self.start("attArr", name, attrs['ref'])
		elif (name in self.handleDict):
			self.start(self.handleDict[name], name, attrs)
		self.writer.startElement(name, attrs)

	def endElement(self, name):
		if (name in self.handleDict):
			self.handleArrObj(name)
			if (self.hierarchy.pop() == 'att'):
				self.hierarchy.pop()
			self.indent -= 1
			if (not self.handleDict[name] == 'attArr'):
				# Finish object
				self.outFile.write("\n")
				for i in range(self.indent):
					self.outFile.write(" ")
				self.outFile.write("}")
		self.writer.endElement(name)

	@staticmethod
	def shortName():
		return 'w-json'

	@staticmethod
	def longName():
		return 'w-json'
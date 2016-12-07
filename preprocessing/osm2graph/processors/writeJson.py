from base.core import ComboOsmProcessor
from writer.JSONWriter import JSONWriter

class WriteJson(ComboOsmProcessor):
	def __init__(self, opts):
		self.writer = JSONWriter(opts)
		
	def shedule(self, opts, shedule, step):
		self.writer.shedule({"life": str(step) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'json'

	@classmethod
	def longName(cls):
		return 'json'
		
	@classmethod
	def getInfo(cls):
		return ("[Writer] Write the output to a JSON-file.", [("out","The file to which the output is writen.")])
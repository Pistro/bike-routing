from base.core import ComboOsmProcessor
from reader.osm.nodeCounter import NodeCounter
from writer.wayCleaner import WayCleaner

class CleanWay(ComboOsmProcessor):
	def __init__(self, opts):
		self.nodeCounter = NodeCounter(opts)
		self.wayCleaner = WayCleaner(opts)
		
	def shedule(self, opts, shedule, step):
		self.nodeCounter.shedule({"life": str(step) + "-end"}, shedule)
		self.wayCleaner.shedule({"life": str(step) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'cwy'

	@classmethod
	def longName(cls):
		return 'clean-way'
		
	@classmethod
	def getInfo(cls):
		return ("[Writer] Remove ways of which not all nodes are present.", None)
from base.core import ComboOsmProcessor
from reader.osm.nodeCounter import NodeCounter
from writer.waySplitter import WaySplitter

class WayToEdge(ComboOsmProcessor):
	def __init__(self, opts):
		self.nodeCounter = NodeCounter(opts)
		self.waySplitter = WaySplitter(opts)
		
	def shedule(self, opts, shedule, step):
		self.nodeCounter.shedule({"life": str(step) + "-end"}, shedule)
		self.waySplitter.shedule({"life": str(step+1) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'we'

	@classmethod
	def longName(cls):
		return 'way-to-edge'
		
	@classmethod
	def getInfo(cls):
		return ("[Read-writer] Convert ways into edges by splitting them at nodes at which multiple ways intersect. The original way id is stored in the new tag wayId.", None)
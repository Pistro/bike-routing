from base.core import ComboOsmProcessor
from reader.osm.nodeCounter import NodeCounter
from writer.nodeCleaner import NodeCleaner

class CleanNode(ComboOsmProcessor):
	def __init__(self, opts):
		self.nodeCounter = NodeCounter(opts)
		self.nodeCleaner = NodeCleaner(opts)
		
	def shedule(self, opts, shedule, step):
		self.nodeCounter.shedule({"life": str(step) + "-end"}, shedule)
		self.nodeCleaner.shedule({"life": str(step+1) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'cn'

	@classmethod
	def longName(cls):
		return 'clean-node'
		
	@classmethod
	def getInfo(cls):
		return ("[Read-writer] Remove all nodes which are not part of any way.", None)
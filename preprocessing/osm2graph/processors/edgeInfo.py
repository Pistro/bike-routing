from base.core import ComboOsmProcessor
from reader.osm.nodeLatLon import NodeLatLon
from writer.nodeMerger import NodeMerger

class EdgeInfo(ComboOsmProcessor):
	def __init__(self, opts):
		self.nodeLatLon = NodeLatLon(opts)
		self.nodeMerger = NodeMerger(opts)
		
	def shedule(self, opts, shedule, step):
		self.nodeLatLon.shedule({"life": str(step) + "-end"}, shedule)
		self.nodeMerger.shedule({"life": str(step) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'ei'
		
	@classmethod
	def longName(cls):
		return 'edge-info'
		
	@classmethod
	def getInfo(cls):
		return ("[Writer] Add the startNode, endNode and length attributes to each edge.", None)
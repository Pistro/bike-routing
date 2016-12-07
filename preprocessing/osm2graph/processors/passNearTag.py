from base.core import ComboOsmProcessor
from reader.osm.nodeLatLon import NodeLatLon
from reader.osm.wayWayNode import WayWayNode
from reader.inter.wayMapper import WayMapper
from reader.inter.passNearReader import PassNearReader
from writer.wayTagUpdater import WayTagUpdater

class PassNearTag(ComboOsmProcessor):
	def __init__(self, opts):
		self.nodeLatLon = NodeLatLon(opts)
		self.wayWayNode = WayWayNode(opts)
		self.wayMapper = WayMapper(opts)
		self.passNearReader = PassNearReader(opts)
		self.wayTagUpdater = WayTagUpdater(opts)
		
	def shedule(self, opts, shedule, step):
		self.nodeLatLon.shedule({"life": str(step)}, shedule)
		self.wayWayNode.shedule({"life": str(step)}, shedule)
		self.wayMapper.shedule({"life": str(step)}, shedule)
		self.passNearReader.shedule({"life": str(step) + "-end"}, shedule)
		self.wayTagUpdater.shedule({"life": str(step+1) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'pn'

	@classmethod
	def longName(cls):
		return 'pass-near'
		
	@classmethod
	def getInfo(cls):
		return ("[Read-writer] Detect whether a way runs near any way of a given file.", [('in', 'An osm-file containing the ways to which ways in the stream are compared.'), ('tag', 'The tag key that has to be assigned to ways that pass near to ways of the given file. The tag value for these ways is 1.')])
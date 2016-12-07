from base.core import ComboOsmProcessor
from reader.osm.nodeLatLon import NodeLatLon
from reader.osm.wayWayNode import WayWayNode
from reader.inter.wayMapper import WayMapper
from reader.inter.poiReader import PoiReader
from writer.wayTagUpdater import WayTagUpdater

class PoiInfo(ComboOsmProcessor):
	def __init__(self, opts):
		self.nodeLatLon = NodeLatLon(opts)
		self.wayWayNode = WayWayNode(opts)
		self.wayMapper = WayMapper(opts)
		self.poiReader = PoiReader(opts)
		self.wayTagUpdater = WayTagUpdater(opts)
		
	def shedule(self, opts, shedule, step):
		self.nodeLatLon.shedule({"life": str(step)}, shedule)
		self.wayWayNode.shedule({"life": str(step)}, shedule)
		self.wayMapper.shedule({"life": str(step)}, shedule)
		self.poiReader.shedule({"life": str(step) + "-end"}, shedule)
		self.wayTagUpdater.shedule({"life": str(step+1) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'poi'

	@classmethod
	def longName(cls):
		return 'poi'
		
	@classmethod
	def getInfo(cls):
		return ("[Read-writer] Detect for each node and closed way from a given file which way from the stream is the closest and count for each way of the stream how many of these points of interest tags are assigned to it.", [("in", "An osm-file containing the nodes and ways to which ways in the stream are compared."), ("remap", "A comma separated list of the tags which must be checked, their value and the new tag on which the number of tag-values must be mapped. If no value is specified, or a * is passed, all tags with a matching key are remapped. If no new tag is specified, tags are remapped onto themselves. E.g.: remap=amenity.bar.nrBars,amenity.cafe")])
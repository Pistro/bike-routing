from base.core import ComboOsmProcessor
from reader.osm.nodeLatLon import NodeLatLon
from reader.osm.wayWayNode import WayWayNode
from reader.inter.wayMapper import WayMapper
from reader.inter.passThroughWayReader import PassThroughWayReader
from reader.inter.passThroughRelReader import PassThroughRelReader
from writer.wayTagUpdater import WayTagUpdater

class PassThroughInfo(ComboOsmProcessor):
	def __init__(self, opts):
		self.nodeLatLon = NodeLatLon(opts)
		self.wayWayNode = WayWayNode(opts)
		self.wayMapper = WayMapper(opts)
		if ("rel" in opts):
			self.forestReader = PassThroughRelReader(opts)
		else:
			self.forestReader = PassThroughWayReader(opts)
		self.wayTagUpdater = WayTagUpdater(opts)
		
	def shedule(self, opts, shedule, step):
		self.nodeLatLon.shedule({"life": str(step)}, shedule)
		self.wayWayNode.shedule({"life": str(step)}, shedule)
		self.wayMapper.shedule({"life": str(step)}, shedule)
		self.forestReader.shedule({"life": str(step) + "-end"}, shedule)
		self.wayTagUpdater.shedule({"life": str(step+1) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'pt'
		
	@classmethod
	def longName(cls):
		return 'pass-through'
		
	@classmethod
	def getInfo(cls):
		return ("[Read-writer] Detect through how many of the closed ways with specified tags in a given file each way in the stream passes.", [("rel", "Optional. Detect whether the ways passes through the relations specified in the files, rather than the ways (relations can have inner and outer bounds)."), ("in", "An osm-file containing the ways (and relations) to which ways in the stream are compared."), ("remap", "A comma separated list of the tags which must be checked, their value and the new tag on which the number of tag-values must be mapped. If no value is specified, or a * is passed, all tags with a matching key are remapped. If no new tag is specified, tags are remapped onto themselves. E.g.: remap=landuse.forest.nrForests,natural.wood")])
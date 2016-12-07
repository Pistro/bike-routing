from base.core import ComboOsmProcessor
from reader.osm.crossWayAttr import CrossWayAttr
from writer.nodeWayTagUpdater import NodeWayTagUpdater

class CrossInfo(ComboOsmProcessor):
	def __init__(self, opts):
		self.crossWayAttr = CrossWayAttr(opts)
		self.nodeWayTagUpdater = NodeWayTagUpdater(opts)
		
	def shedule(self, opts, shedule, step):
		self.crossWayAttr.shedule({"life": str(step) + "-end"}, shedule)
		self.nodeWayTagUpdater.shedule({"life": str(step+1) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'cr'

	@classmethod
	def longName(cls):
		return 'cross'
		
	@classmethod
	def getInfo(cls):
		return ("[Read-writer] Count how many neighbouring edges contain given tags.", [("remap", "A comma separated list of the tags which must be checked, their value and the new tag on which the number of tag-values must be mapped. If no value is specified, or a * is passed, all tags with a matching key are remapped. If no new tag is specified, tags are remapped onto themselves. E.g.: remap=highway.primary.nrPrimaryNeighbours,highway.*.nrNeighbours"), ("endings", "Optional. Check only for neighbouring ways at the start and end points of a way. This brings benefits in memry and performance and is useful when the ways are split up into edges.")])
		

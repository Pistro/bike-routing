from base.core import ComboOsmProcessor
from reader.osm.relWayAttr import RelWayAttr
from writer.wayTagUpdater import WayTagUpdater

class InheritInfo(ComboOsmProcessor):
	def __init__(self, opts):
		self.relWayAttr = RelWayAttr(opts)
		self.wayTagUpdater = WayTagUpdater(opts)
		
	def shedule(self, opts, shedule, step):
		self.relWayAttr.shedule({"life": str(step) + "-end"}, shedule)
		self.wayTagUpdater.shedule({"life": str(step+1) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'inh'

	@classmethod
	def longName(cls):
		return 'inherit'
		
	@classmethod
	def getInfo(cls):
		return ("[Read-writer] Count how many relations with given tags contain a way.", [('remap', 'A comma separated list of the tags which must be checked, their value and the new tag on which the number of tag-values must be mapped. If no value is specified, or a * is passed, all tags with a matching key are remapped. If no new tag is specified, tags are remapped onto themselves. E.g.: remap=route.bicycle.nrBikeroutes,route.foot will result in a tag nrBikeroutes containing the number of relations with tags with key route and value bike that pass through a way and a tag foot containing the number of relations with tags with key route and value foot that pass through a way.')])
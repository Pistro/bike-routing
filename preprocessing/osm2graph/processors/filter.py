from base.core import ComboOsmProcessor
from writer.elementFilter import ElementFilter

class Filter(ComboOsmProcessor):
	def __init__(self, opts):
		self.elementFilter = ElementFilter(opts)
		
	def shedule(self, opts, shedule, step):
		self.elementFilter.shedule({"life": str(step) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'filter'
		
	@classmethod
	def longName(cls):
		return 'filter'
		
	@classmethod
	def getInfo(cls):
		return ("[Writer] Filter out elements from the stream", [("rules", "[Standard: -*] A comma-separated list, specifying rules about elements should be passed or filtered out. Each rule starts with either + or -, in which + allows an element to pass and - filters out an element (and all subelements). A subelement b of an element a is adressed as a.b, * is a wildcard for all subelements. No rule earlier in the list should be an exception of a rule later in the list. E.g.: rules=+*,-osm.node,-osm.way.attr.version means: let all elements pass, except for the osm nodes and the version attribute of the osm ways.")])	
from base.core import OsmReader
from base.core import ComboOsmProcessor
from writer.wayUpdater import WayUpdater
from writer.nodeUpdater import NodeUpdater
import gdal, osr, ogr, struct
from gdalconst import *
from extra.geoTIFFReader import GeoTIFFReader

class HeightReader(OsmReader):
	def __init__(self, opts):
		self.share = opts['share']
		self.reader = GeoTIFFReader(opts['in'])
		self.nodes = dict()
		
	def startElement(self, name, attrs):
		if name=='node':
			lat = attrs['lat']
			lon = attrs['lon']
			self.nodes[int(attrs['id'])] = self.reader.getHeight(float(lat), float(lon))
		self.writer.startElement(name, attrs)
				
	def exports(self):
		return {'dict-node-height_' + self.share: 'nodes'}
		
	@classmethod
	def shortName(cls):
		return 'nH'
		
	@classmethod
	def longName(cls):
		return 'nodeHeight'

class WayHeightAdder(WayUpdater):
	def __init__(self, opts):
		super().__init__(opts)

	def updateWay(self, attrs, tags, nodes):
		heightDif = 0
		for i in range(0, len(nodes)-1):
			heightDif += abs(self.nodesH[nodes[i]]-self.nodesH[nodes[i+1]])
		tags['height_dif'] = str(round(heightDif*100)/100)
		return (attrs, tags)

	def imports(self):
		return {'dict-node-height_' + self.share: 'nodesH'}

	@classmethod
	def shortName(cls):
		return 'wha'
		
	@classmethod
	def longName(cls):
		return 'wayHeightAdder'
		
class NodeHeightAdder(NodeUpdater):
	def __init__(self, opts):
		super().__init__(opts)

	def updateNode(self, attrs, tags):
		tags['height'] = self.nodesH[int(attrs['id'])]
		return (attrs, tags)

	def imports(self):
		return {'dict-node-height_' + self.share: 'nodesH'}

	@classmethod
	def shortName(cls):
		return 'nha'
		
	@classmethod
	def longName(cls):
		return 'nodeHeightAdder'
		
class AddHeight(ComboOsmProcessor):
	def __init__(self, opts):
		self.nodeHeight = HeightReader(opts)
		self.wayHeightAdder = WayHeightAdder(opts)
		self.nodeHeightAdder = NodeHeightAdder(opts)
		
	def shedule(self, opts, shedule, step):
		self.nodeHeight.shedule({"life": str(step) + "-end"}, shedule)
		self.wayHeightAdder.shedule({"life": str(step) + "-end"}, shedule)
		self.nodeHeightAdder.shedule({"life": str(step) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'h'
		
	@classmethod
	def longName(cls):
		return 'height'
		
	@classmethod
	def getInfo(cls):
		return ("[Writer] Assign a heightDif attribute to each edge, containing the height difference along the edge.", [("in", "A Geo-TIFF file containing the heights for each node of the stream.")])
from base.core import OsmReader
from base.largeDicts import CoordinateDict

class NodeLatLon(OsmReader):
	def __init__(self, opts):
		self.share = opts['share']
		self.nodes = CoordinateDict()
		
	def startElement(self, name, attrs):
		if name=='node':
			lat = attrs['lat']
			lon = attrs['lon']
			self.nodes[int(attrs['id'])] = (float(lat), float(lon))
		self.writer.startElement(name, attrs)
					
	def exports(self):
		return {'dict-node-latLon_' + self.share: 'nodes'}
		
	@staticmethod
	def shortName():
		return 'nLL'

	@staticmethod
	def longName():
		return 'nodeLatLon'
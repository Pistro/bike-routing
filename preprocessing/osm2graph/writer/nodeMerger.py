from haversine import haversine
from writer.wayUpdater import WayUpdater

class NodeMerger(WayUpdater):
	def __init__(self, opts):
		super().__init__(opts)

	def updateWay(self, attrs, tags, nodes):
		length = 0
		for i in range(0, len(nodes)-1):
			start = self.nodesLL[nodes[i]]
			stop = self.nodesLL[nodes[i+1]]
			length += haversine(start, stop)*1000
		tags['length'] = str(round(length*100)/100)
		tags['start_node'] = str(nodes[0])
		tags['end_node'] = str(nodes[len(nodes)-1])
		return (attrs, tags)

	def imports(self):
		return {'dict-node-latLon_' + self.share: 'nodesLL'}

	@staticmethod
	def shortName():
		return 'nm'

	@staticmethod
	def longName():
		return 'nodeMerger'
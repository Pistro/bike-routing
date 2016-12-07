from writer.wayUpdater import WayUpdater

class NodeWayTagUpdater(WayUpdater):
	def __init__(self, opts):
		super().__init__(opts)
		self.endings = 'endings' in opts

	def updateWay(self, wayAttrs, tags, wayNodes):
		if self.endings and len(wayNodes):
			wayNodes = [wayNodes[0], wayNodes[len(wayNodes)-1]]
		for node in wayNodes:
			if (node in self.updateAttr):
				keys = self.updateAttr[node]
				for key in keys:
					if not key in tags:
						tags[key] = 0
					tags[key] += 1
		return (wayAttrs, tags)
			
	def imports(self):
		return {'dict-node-wayAttr_' + self.share: 'updateAttr'}
		
	@staticmethod
	def shortName():
		return 'nWTU'

	@staticmethod
	def longName():
		return 'node-WayTagUpdater'
from writer.wayUpdater import WayUpdater

class WayTagUpdater(WayUpdater):
	def __init__(self, opts):
		super().__init__(opts)

	def updateWay(self, attrs, tags, nodes):
		wayId = attrs['id']
		if (wayId in self.wayUpdate):
			extraTags = self.wayUpdate[wayId]
		else:
			extraTags = dict()
		for attr in extraTags:
			if attr in tags:
				try:
					tags[attr]  = str(int(tags[attr])+extraTags[attr])
				except ValueError:
					tags[attr] = str(extraTags[attr])
			else:
				tags[attr] = str(extraTags[attr])
		return (attrs, tags)
			
	def imports(self):
		return {'dict-way-wayAttr_' + self.id: 'wayUpdate'}
		
	@staticmethod
	def shortName():
		return 'wTU'

	@staticmethod
	def longName():
		return 'wayTagUpdater'
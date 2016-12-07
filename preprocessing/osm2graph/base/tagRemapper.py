class TagRemapper():
	def __init__(self, attrMapString):
		self.map = dict()
		tags = attrMapString.split(",")
		for tag in tags:
			nameValueNew = tag.split(".", 2)
			while (len(nameValueNew) < 3):
				nameValueNew.append('*')
			self.map[(nameValueNew[0], nameValueNew[1])] = nameValueNew[2]

	def addMatches(self, tagAttrs, keys):
		for pair in self.map:
			(relKey, relValue) = pair
			if (tagAttrs['k'] == relKey):
				if (tagAttrs['v'] == relValue or relValue == "*"):
					newValue = self.map[pair]
					if (newValue == '*'):
						keys.add(tagAttrs['v'])
					else:
						keys.add(newValue)
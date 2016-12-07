class Profile():
	def getConstantScore(self, tags):
		return 0
		
	def getLinearScore(self, tags):
		return 0
		
	def addTags(self, wayAttrs, tags):
		return tags
		
	@classmethod
	def getName(cls):
		raise NotImplementedError("Each Profile should override the 'getName' function" + cls.__name__)
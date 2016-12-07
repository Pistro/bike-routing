from base.core import OsmWriter

class ElementFilter(OsmWriter):
	def __init__(self, opts):
		rules = opts['rules'].split(',')
		exceptions = { 'st_*': False, 'write': True } # Pol is policy for exact matches, star is policy for unspecified children
		for rule in rules:
			accept = (rule[0] == '+')
			rule = rule[1:]
			currentNode = exceptions
			condition = None
			while not (rule == '' or rule == '*'):
				nodeRest = rule.split('.', 1)
				nextCond = nodeRest[0].split(':', 1)
				nextNode = nextCond[0]
				if len(nextCond)==2:
					condition = nextCond[1]
				if not nextNode in currentNode:
					currentNode[nextNode] = dict({'prev': currentNode, 'st_*': currentNode['st_*'], 'st_pol': currentNode['st_*']})
				currentNode = currentNode[nextNode]
				if len(nodeRest) == 1:
					rule = ''
				else:
					rule = nodeRest[1]
			if (condition):
				if (not 'conditions' in currentNode):
					currentNode['conditions'] = list()
				if ('~' in condition):
					kv = condition.split('~', 1)
					currentNode['conditions'].append((accept, rule=='*', kv[0], kv[1]))
				else:
					currentNode['conditions'].append((accept, rule=='*', condition))
			else:
				if (rule == '*' or (not accept)):
					currentNode['st_*'] = accept
				if not (rule == '*'):
					currentNode['st_pol'] = accept
		exceptions['*'] = exceptions['st_*']
		self.current = exceptions
		self.extra = 0
				
	def startElement(self, name, attrs):
		if (name in self.current and self.current['write']):
			self.current = self.current[name]
			self.current['*'] = self.current['st_*']
			self.current['pol'] = self.current['st_pol']
			if 'conditions' in self.current:
				for condition in self.current['conditions']:
					if (condition[2] in attrs and (len(condition)==3 or attrs[condition[2]] == condition[3])):
						if condition[1]:
							self.current['*'] = condition[0]
						else:
							self.current['pol'] = condition[0]
		else:
			self.extra += 1
		
		if ((self.current['*'] and self.current['write']) if self.extra else self.current['pol']):
			# Write the element
			attrs = self.filterAttrs(attrs)
			self.writer.startElement(name, attrs)
			self.current['write'] = True
		else:
			self.current['write'] = False
		
	def filterAttrs(self, attrs):
		if 'attr' in self.current and (not self.extra):
			attributeRules = self.current['attr']
			out = dict()
			attrs = dict(attrs)
			for attr in attrs:
				if (attr in attributeRules):
					if (attributeRules[attr]['st_pol']):
						out[attr] = attrs[attr]
				elif (attributeRules['st_*']):
					out[attr] = attrs[attr]
			return out
		else:
			if self.current['*']:
				return attrs
			else:
				return dict()
				
	def endElement(self,name):
		if ((self.current['*'] and self.current['write']) if self.extra else self.current['pol']):
			self.writer.endElement(name)

		if (self.extra):
			self.extra -= 1
		else:
			self.current = self.current['prev']
		
	@staticmethod
	def shortName():
		return 'ef'

	@staticmethod
	def longName():
		return 'elementFilter'
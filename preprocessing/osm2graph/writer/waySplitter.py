from writer.wayUpdater import WayUpdater

class WaySplitter(WayUpdater):
	def __init__(self, opts):
		super().__init__(opts)

	def startDocument(self):
		self.counter = 1
		self.writer.startDocument()

	def endElement(self, name):
		if name=='way':
			subId = 0
			self.nodes.append(-1)
			segmentNodes = []
			self.tags['original_id'] = self.attrs['id']
			curNode = None
			while len(self.nodes)>0:
				lastNode = curNode
				curNode = self.nodes.pop(0)
				if (curNode in self.inbetweenNodes or curNode in self.crossings) and not (curNode == lastNode):
					#Geldige node
					segmentNodes.append(curNode)
				else:
					curNode = None
					
				if ((curNode == None) or (curNode in self.crossings and len(segmentNodes)>1)):
					#Start een nieuwe weg
					if (len(segmentNodes)>1):
						#Voeg eerst oude weg toe
						self.attrs['id'] = str(self.counter)
						self.tags['original_subid'] = str(subId)
						self.writer.startElement('way', self.attrs)
						self.counter += 1
						for node in segmentNodes:
							self.writer.startElement('nd', {'ref': str(node)})
							self.writer.endElement('nd')
						for tag in self.tags:
							self.writer.startElement('tag', {'k': tag, 'v': self.tags[tag]})
							self.writer.endElement('tag')
						self.writer.endElement('way')
						subId += 1
					if (curNode == None):
						segmentNodes = []
					else:
						segmentNodes = [segmentNodes[len(segmentNodes)-1]]
			self.attrs = None
		elif (not self.attrs):
			self.writer.endElement(name)
			
	def imports(self):
		return {'set-crossings_' + self.share: 'crossings', 'set-inbetweenNodes_' + self.share: 'inbetweenNodes'}
		
	@staticmethod
	def shortName():
		return 'wSpl'

	@staticmethod
	def longName():
		return 'waySplitter'
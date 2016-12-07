import array

class CoordinateDict():
	def __init__(self):
		self.indexMap = dict()
		self.coords = array.array('d')

	def __getitem__(self, key):
		if key in self.indexMap:
			pos = self.indexMap[key]
			return (self.coords[pos], self.coords[pos+1])
		else:
			raise KeyError(key)
			
	def __setitem__(self, key, coords):
		if key in self.indexMap:
			pos = self.indexMap[key]
			self.coords[pos] = coords[0]
			self.coords[pos+1] = coords[1]
		else:
			self.indexMap[key] = len(self.coords)
			self.coords.append(coords[0])
			self.coords.append(coords[1])
			
	def __len__(self):
		return len(self.indexMap)
		
	def __contains__(self, index):
		return index in self.indexMap
		
	def __iter__(self):
		return self.indexMap.__iter__()
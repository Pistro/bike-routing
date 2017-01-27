from rtree import index
from base.core import InterReader
from haversine import haversine
from shapely.geometry import Point
from shapely.geometry import LineString
from shapely.geometry import Polygon

class WayMapper(InterReader):
	def __init__(self, opts):
		self.share = opts['share']
		self.me = self

	def execute(self):
		def generator():
			addedSegments = 0
			for id in self.ways:
				wayNodes = self.ways[id]
				for i in range(len(wayNodes)-1):
					if wayNodes[i] in self.nodes and wayNodes[i+1] in self.nodes:
						start = self.nodes[wayNodes[i]]
						stop = self.nodes[wayNodes[i+1]]
						switched = False
						if (start[0]>stop[0]):
							tmp = stop[0]
							stop = (start[0], stop[1])
							start = (tmp, start[1])
							switched = (not switched)
						if (start[1]>stop[1]):
							tmp = stop[1]
							stop = (stop[0], start[1])
							start = (start[0], tmp)
							switched = (not switched)
						yield (addedSegments, (start[0], start[1], stop[0], stop[1]), (switched, id))
						addedSegments += 1
		self.index = index.Index(generator())
		del self.nodes
		del self.ways
		
	def getClosestWay(self, points, searchDist):
		(lat, lon) = points[0]
		mPerLat = haversine((lat-0.5, lon), (lat+0.5, lon))*1000
		mPerLon = haversine((lat, lon-0.5), (lat, lon+0.5))*1000
		rescaled = list()
		for point in points:
			rescaled.append((mPerLat*point[0], mPerLon*point[1]))
		if (len(points) == 1):
			c = Point(rescaled[0][0], rescaled[0][1])
		else:
			c = Polygon(rescaled)
		(minLat, minLon, maxLat, maxLon) = self.getWindow(points)
		minWayDist = float('inf')
		minWay = None
		researched = set()
		for searchDInd in range(len(searchDist)):
			searchD = searchDist[searchDInd]
			if searchDInd == 0 or (searchDInd > 0 and minWayDist>searchDist[searchDInd-1]):
				halfLat = searchD/mPerLat
				halfLon = searchD/mPerLon
				overlaps = self.index.intersection((minLat-halfLat, minLon-halfLon, maxLat+halfLat, maxLon+halfLon), objects=True)	
				for waySegment in overlaps:
					if (not waySegment in researched):
						researched.add(waySegment)
						obj = waySegment.object
						box = list(waySegment.bbox)
						if obj[0]:
							(box[1], box[3]) = (box[3], box[1])
						ls = LineString([(mPerLat*box[0], mPerLon*box[1]), (mPerLat*box[2], mPerLon*box[3])])
						d = c.distance(ls)
						if (d<minWayDist):
							minWayDist = d
							minWay = obj[1]
		if (minWayDist>searchD):
			return None
		return minWay
		
	def getCloseWays(self, points, searchD):
		(lat, lon) = points[0]
		mPerLat = haversine((lat-0.5, lon), (lat+0.5, lon))*1000
		mPerLon = haversine((lat, lon-0.5), (lat, lon+0.5))*1000
		rescaled = list()
		for point in points:
			rescaled.append((mPerLat*point[0], mPerLon*point[1]))
		if (len(points) == 1):
			c = Point(rescaled[0][0], rescaled[0][1])
		else:
			c = LineString(rescaled)
		found = set()
		halfLat = searchD/mPerLat
		halfLon = searchD/mPerLon
		(minLat, minLon, maxLat, maxLon) = self.getWindow(points)
		overlaps = self.index.intersection((minLat-halfLat, minLon-halfLon, maxLat+halfLat, maxLon+halfLon), objects=True)
		for waySegment in overlaps:
			obj = waySegment.object
			if (not obj[1] in found):
				box = list(waySegment.bbox)
				if obj[0]:
					(box[1], box[3]) = (box[3], box[1])
				ls = LineString([(mPerLat*box[0], mPerLon*box[1]), (mPerLat*box[2], mPerLon*box[3])])
				d = c.distance(ls)
				if (d<searchD):
					found.add(obj[1])
		return found

	def hasCloseWay(self, points, searchDist):
		(lat, lon) = points[0]
		mPerLat = haversine((lat-0.5, lon), (lat+0.5, lon))*1000
		mPerLon = haversine((lat, lon-0.5), (lat, lon+0.5))*1000
		rescaled = list()
		for point in points:
			rescaled.append((mPerLat*point[0], mPerLon*point[1]))
		if (len(points) == 1):
			c = Point(rescaled[0][0], rescaled[0][1])
		else:
			c = LineString(rescaled)
		(minLat, minLon, maxLat, maxLon) = self.getWindow(points)
		researched = set()
		maxDist = searchDist[len(searchDist)-1]
		for searchDInd in range(len(searchDist)):
			searchD = searchDist[searchDInd]
			halfLat = searchD/mPerLat
			halfLon = searchD/mPerLon
			overlaps = self.index.intersection((minLat-halfLat, minLon-halfLon, maxLat+halfLat, maxLon+halfLon), objects=True)	
			for waySegment in overlaps:
				if (not waySegment in researched):
					researched.add(waySegment)
					obj = waySegment.object
					box = list(waySegment.bbox)
					if obj[0]:
						(box[1], box[3]) = (box[3], box[1])
					ls = LineString([(mPerLat*box[0], mPerLon*box[1]), (mPerLat*box[2], mPerLon*box[3])])
					d = c.distance(ls)
					if (d<maxDist):
						return True
		return False

	def getWindow(self, outerPoints):
		minLat = min(outerPoints, key=lambda x:x[0])[0]
		maxLat = max(outerPoints, key=lambda x:x[0])[0]
		minLon = min(outerPoints, key=lambda x:x[1])[1]
		maxLon = max(outerPoints, key=lambda x:x[1])[1]
		return (minLat, minLon, maxLat, maxLon)

	def getInnerWays(self, outerBoundaries, innerBoundaries=[]):
		overlaps = list()
		outerBounds = list()
		for outerBoundary in outerBoundaries:
			outerBounds.append(Polygon(outerBoundary))
			overlaps += list(self.index.intersection(self.getWindow(outerBoundary), objects=True))
		innerBounds = list()
		for innerBoundary in innerBoundaries:
			innerBounds.append(Polygon(innerBoundary))
		found = set() #wayIds of added ways
		done = set() #WaysegmentIds of segments doen
		for waySegment in overlaps:
			obj = waySegment.object
			if (not obj[1] in found) and (not waySegment.id in done):
				done.add(waySegment.id)
				box = list(waySegment.bbox)
				if obj[0]:
					(box[1], box[3]) = (box[3], box[1])
				ls = LineString([(box[0], box[1]), (box[2], box[3])])
				add = False
				for ob in outerBounds:
					try:
						if (ls.intersects(ob)):
							add = True
							break
					except TopologicalError:
						pass
				if (add):
					for ib in innerBounds:
						if (ib.contains(ls)):
							add = False
							break
				if (add):
					found.add(obj[1])
		return found
		
	def imports(self):
		return {'dict-way-nodes_' + self.share: 'ways', 'dict-node-latLon_' + self.share: 'nodes'}
		
	def exports(self):
		return {'wayMapper_' + self.share: 'me'}
		
	@staticmethod
	def shortName():
		return 'wMap'

	@staticmethod
	def longName():
		return 'wayMap'
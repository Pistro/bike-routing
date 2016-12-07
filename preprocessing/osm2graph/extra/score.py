from base.core import ComboOsmProcessor
from writer.wayUpdater import WayUpdater
import math, os, sys
sys.path.append(os.path.abspath(''))
from scores import *

class ScoreCalculator(WayUpdater):
	def __init__(self, opts):
		super().__init__(opts)
		profiles = profile.Profile.__subclasses__()
		self.profile = None
		for p in profiles:
			if (p.getName() == opts['profile']):
				self.profile = p()
				break
		if (self.profile == None):
			raise ValueError(opts['profile']  + " is no name of a profile subclass")
		if ('poiWeight' in opts):
			self.poiWeight = float(opts['poiWeight'])
		if ('qWeight' in opts):
			self.qWeight = float(opts['qWeight'])
			
	def updateWay(self, wayAttrs, tags, wayNodes):
		tags = self.profile.addTags(wayAttrs, tags)
		return (wayAttrs, tags)
		
	@classmethod
	def shortName(cls):
		return 'score'
		
	@classmethod
	def longName(cls):
		return 'scoreCalculator'
		
class BicycleWeights(ComboOsmProcessor):
	def __init__(self, opts):
		self.bwc = ScoreCalculator(opts)
		
	def shedule(self, opts, shedule, step):
		self.bwc.shedule({"life": str(step) + "-end"}, shedule)
		
	@classmethod
	def shortName(cls):
		return 'w'
		
	@classmethod
	def longName(cls):
		return 'weights'
		
	@classmethod
	def getInfo(cls):
		return ("[Writer] Assign a score attribute to each edge, containing how pleasant an edge is for a bicyclist.", None)
from django.db import models
from django.utils import timezone
import datetime

class Node(models.Model):
	id = models.IntegerField('id', primary_key=True)
	lat = models.FloatField('latitude')
	lon = models.FloatField('longitude')
	height = models.DecimalField('height', max_digits=7, decimal_places=2, default=0)
	
	def __str__(self):
		return str(self.id)
		
class Tag(models.Model):
	id = models.IntegerField('id', primary_key=True)
	key = models.TextField('key')
	value = models.TextField('value')
	
	def __str__(self):
		return str(self.key) + ": " + str(self.value)
	
class Way(models.Model):
	id = models.IntegerField('id', primary_key=True)
	orgWayId = models.IntegerField('orgWayId')
	orgWaySubId = models.IntegerField('orgWaySubId')
	tags = models.ManyToManyField(Tag, 'tags', blank=True)
	nodes = models.ManyToManyField(Node, 'nodes', through='Membership')
	
	def wayNodes(self):
		nodes = self.nodes.all()
		out = len(nodes)*[None]
		for node in nodes:
			memberships = node.membership_set.filter(way=self.id)
			for membership in memberships:
				out[membership.pos] = node
		return out
		
	def wayNodeIds(self):
		out = self.wayNodes()
		for nodeIndex in range(len(out)):
			out[nodeIndex] = out[nodeIndex].id
		return out
		
	def wayTags(self):
		return [str(t) for t in self.tags.all()]
		
	def __str__(self):
		return str(self.id)
		
class Membership(models.Model):
	way = models.ForeignKey(Way)
	node = models.ForeignKey(Node)
	pos = models.IntegerField('pos')
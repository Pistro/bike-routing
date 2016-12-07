from django.shortcuts import get_object_or_404, render
from django.http import HttpResponse, HttpResponseRedirect
from django.http import Http404
from django.core.urlresolvers import reverse
from django.views import generic
from .models import Way
import json
from haversine import haversine

import os, sys
sys.path.append(os.path.abspath(os.path.join('..', 'preprocessing')))
from scores import *

def index(request):
    return render(request, 'showRoutes/index.html')
	
def height(request):
    return render(request, 'showRoutes/height.html')
	
def getProfile(profileName):
	profiles = profile.Profile.__subclasses__()
	out = None
	for p in profiles:
		if (p.getName() == profileName):
			out = p()
			break
	if (out == None):
		raise ValueError(profileName  + " is no name of a profile subclass")
	return out
	
def addWayInformation(out, wayHolders):
	p = getProfile('bikeProfile')
	waySet = set()
	for route in wayHolders:
		routeWays = route['ways']
		for way in routeWays:
			waySet.add(way)
	foundWays = set()
	for way in waySet:
		foundWays.add(Way.objects.filter(id=way)[0])
	nodeDict = dict()
	if not 'ways' in out:
		out['ways'] = dict()
	ways = out['ways']
	for foundWay in foundWays:
		if (not foundWay.id in ways):
			ways[foundWay.id] = dict()
		way = ways[foundWay.id]
		way['id'] = foundWay.id
		way['orgWayId'] = foundWay.orgWayId
		way['orgWaySubId'] = foundWay.orgWaySubId
		way['nodes'] = list()
		wayNodes = foundWay.wayNodes()
		for node in wayNodes:
			nodeDict[node.id] = node
			way['nodes'].append(node.id)
		length = 0
		for i in range(0, len(wayNodes)-1):
			start = wayNodes[i]
			stop = wayNodes[i+1]
			length += haversine((start.lat, start.lon), (stop.lat, stop.lon))*1000
		foundWayTags = foundWay.tags.all()
		wayTags = dict()
		for tag in foundWayTags:
			wayTags[tag.key] = tag.value
		wayTags['length'] = length
		wayTags = p.addTags(way, wayTags)
		way['tags'] = wayTags
	nodes = dict()
	for nodeId in nodeDict:
		foundNode = nodeDict[nodeId]
		nodes[nodeId] = dict({'lat': foundNode.lat, 'lng': foundNode.lon, 'height': float(foundNode.height)})
	out['nodes'] = nodes

def detail(request):
	out = json.loads(request.FILES['file'].read().decode("utf-8"))
	if 'trees' in out:
		addWayInformation(out, out['trees'])
		return render(request, 'showRoutes/trees.html', {'routeString': json.dumps(out)})
	elif 'routes' in out:
		addWayInformation(out, out['routes'])
		return render(request, 'showRoutes/routes.html', {'routeString': json.dumps(out)})
	else:
		pass
		
def heightDetail(request):
	out = json.loads(request.FILES['file'].read().decode("utf-8"))
	addWayInformation(out, out['routes'])
	return render(request, 'showRoutes/routes2.html', {'routeString': json.dumps(out)})
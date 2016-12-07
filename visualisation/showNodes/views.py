from django.shortcuts import get_object_or_404, render
from django.http import HttpResponse, HttpResponseRedirect
from django.http import Http404
from django.core.urlresolvers import reverse
from django.views import generic
import json

def index(request):
    return render(request, 'showNodes/index.html')
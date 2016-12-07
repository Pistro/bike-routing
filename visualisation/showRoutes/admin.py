from django.contrib import admin

from .models import *

class NodeAdmin(admin.ModelAdmin):
	fields = ('id', ('lat', 'lon'), 'height')
	list_display = ('id', 'lat', 'lon', 'height')
	
class WayAdmin(admin.ModelAdmin):
    list_display = ('id', 'orgWayId', 'orgWaySubId', 'wayNodeIds', 'wayTags')
	
class TagAdmin(admin.ModelAdmin):
    list_display = ('id', 'key', 'value')

class MembershipAdmin(admin.ModelAdmin):
    list_display = ('way', 'node', 'pos')

admin.site.register(Node, NodeAdmin)
admin.site.register(Way, WayAdmin)
admin.site.register(Tag, TagAdmin)
admin.site.register(Membership, MembershipAdmin)
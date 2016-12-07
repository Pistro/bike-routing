from django.conf.urls import url

from . import views

urlpatterns = [
    url(r'^$', views.index, name='index'),
    url(r'^detail/$', views.detail, name='detail'),
    url(r'^height/$', views.height, name='height'),
    url(r'^heightDetail/$', views.heightDetail, name='heightDetail')
]
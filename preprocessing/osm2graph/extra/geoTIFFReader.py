import os, gdal, osr, ogr, struct
from gdalconst import *

class GeoTIFFReader:
    def __init__(self, filename):
        self.dataset = gdal.Open(filename, GA_ReadOnly)
        self.gt = self.dataset.GetGeoTransform()
        self.heightBand = self.dataset.GetRasterBand(1)
        wkt = self.dataset.GetProjection()
        eudemSpatialRef = osr.SpatialReference()
        eudemSpatialRef.ImportFromWkt(wkt)
        stSpatialRef = osr.SpatialReference()
        stSpatialRef.ImportFromEPSG(4326)    
        self.stToEudem = osr.CoordinateTransformation(stSpatialRef, eudemSpatialRef)

    def transform(self, transformation, x, y):
        point = ogr.Geometry(ogr.wkbPoint)
        point.AddPoint(x, y)
        point.Transform(transformation)
        return (point.GetX(), point.GetY())

    def getHeight(self, lat, lon):
        x, y = self.transform(self.stToEudem, lon, lat)
        px = (x - self.gt[0]) / self.gt[1]
        py = (y - self.gt[3]) / self.gt[5]
        structval = self.heightBand.ReadRaster(int(px), int(py), 2, 2, 2, 2, GDT_Float32)
        heights = struct.unpack('f'*4, structval)
        dx = px - int(px)
        dy = py - int(py)
        # Interpolate in x direction
        p1 = (1-dx)*heights[0]+dx*heights[1]
        p2 = (1-dx)*heights[2]+dx*heights[3]
        # Interpolate in y direction
        height = (1-dy)*p1+dy*p2
        return round(100*height)/100
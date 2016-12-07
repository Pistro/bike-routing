# Preprocessing
## Dependencies
 - Osmosis: This program is used to extract information on different kinds of features from the osm data. The executable should be added to your PATH variable.
	- Windows: Download the latest version [here](http://wiki.openstreetmap.org/wiki/Osmosis/Quick_Install_(Windows)), unzip it and add the bin subfolder to your path. It may be necessary to open the osmosis.bat file and change PLEXUS_CP so that it matches the attached plexus jar in lib\default.
	- [Other](http://wiki.openstreetmap.org/wiki/Osmosis/Installation)
 - Osm2graph: This program matches features from one or more osm data sources and converts the osm representation into a graph representation. Requirements of osm2graph and additional info can be found on [this page](https://github.ugent.be/pkstroob/bike-routing/blob/master/preprocessing/osm2graph.md).
 
## Input data
 - The preprocessing algorithm accepts height data as a single GeoTIFF-file.
   The [EU-DEM](http://www.eea.europa.eu/data-and-maps/data/eu-dem#tab-european-data) is a useful source for European height data. Height data is provided through several TIFF files. Using [gdalinfo](http://www.gdal.org/gdalinfo.html), it is easy to find the corner coordinates of the tile to which a GeoTIFF-file corresponds. This is a part of the [GDAL (Geospatial Data Abstraction Library) project](http://www.gdal.org/index.html). 
   Since the algorithm accepts only a single height data file and a region may lay on the border of several tiles of height data, it may be necessary to merge information. The [gdal_merge.py](http://www.gdal.org/gdal_merge.html) script, which is also a part of the [GDAL project](http://www.gdal.org/index.html) can be used to merge GeoTIFF data.
   As a shortcut, we provide a dataset containing the height data of [Belgium](https://www.dropbox.com/s/glqgm0gmvwyduse/belgium_tif.tar.gz) and [France](https://www.dropbox.com/s/remy2rcuy0no83g/france_tif.tar.gz).
 - The osm data of the region that is processed should also be provided as an .osm.pbf file. Via [geofabrik](http://download.geofabrik.de/), osm data can be downloaded in this format for regions of entire continents.
 
## Execution
Before starting the preprocessing, a folder 'source' should be created in the main bike-routing directory. The osm data file should be moved into this folder and should be named 'regionName.osm.pbf'. Similarly, the height data file should be moved into this folder and be named 'regionName.tif'.
To start the preprocessing, open a command prompt in the preprocessing folder and execute:
 - For Windows: `bash\preprocess.bat regionName`
 - For Unix: `bash bash/preprocess.sh regionName`

The preprocessing process will create several files in the source directory:
 - regionName_way.osm: This file contains the entire road network of the region. This file is of no use after the preprocessing step.
 - regionName_primary.osm: This file contains all primary ways of the region. It is used for checking which roads are close to primary ways. This file is of no use after the preprocessing step.
 - regionName_secondary.osm: This file contains all secondary ways of the region. It is used for checking which roads are close to secondary ways. This file is of no use after the preprocessing step.
 - regionName_tertiary.osm: This file contains all tertiary ways of the region. It is used for checking which roads are close to tertiary ways. This file is of no use after the preprocessing step.
 - regionName_poi.osm: This file contains all points of interest (POIs) in the region. It is used for matching POIs to the closest nearby road. This file is of no use after the preprocessing step.
 - regionName_forest_way.osm: This file contains all forests without open spaces in the region. It is used for checking whether a road passes through a forest. This file is of no use after the preprocessing step.
 - regionName_forest_rel.osm: This file contains all forests with open spaces in the region. It is used for checking whether a road passes through a forest. This file is of no use after the preprocessing step.
 - regionName_water.osm: This file contains all waterways, lakes and reservoirs in the region. It is used for checking whether a road passes is close to water. This file is of no use after the preprocessing step.
 - regionName_way.osm: Similar to regionName_way.osm, this file contains the entire roadnetwork of the region. However, here roads are split up at every interesction, leading to a graph representation. This file is of no use after the preprocessing step.
 - regionName_inter.osm: This file contains a graph representation of the entire roadnetwork of the region. It also contains all extra information of the data enrichment: whether edges are close to water, pass through forests,... Height data is also included. This file does not yeat contains perceived edge weights, but is used as an unput to calculate edge weights. As a result, the file is very useful when the edge weight functions are adapted, to avoid redoing the data enrichment.
 - regionName_fixture.xml: This file can be used to create the [visualisation database](https://github.ugent.be/pkstroob/bike-routing/tree/master/visualisation).
 - regionName.wgr: This file contains a graph representation of the entire roadnetwork of the region, including perceived weights. The file serves as an input for the [routing step](https://github.ugent.be/pkstroob/bike-routing/tree/master/routing).
 
After preprocessing the Belgian dataset, some subregions can be extracted by executing `bash\windows\extractRegionsBelgium.bat` (for Windows) or `bash bash/unix/extractRegionsBelgium.sh` (for Unix).
Using the preprocessed data in the `belgium_inter.osm` file, similar 'intermediate' files are created for east-flanders, ghent and geraardsbergen. For each of these regions, a weighted graph representation is also generated.
Additionaly, for each region, a json file is created that is useful for studying the relation between perceived weight and walking distance. The file can be visualised using the 'pleasantness' page described in [visualisations](https://github.ugent.be/pkstroob/bike-routing/tree/master/visualisation).

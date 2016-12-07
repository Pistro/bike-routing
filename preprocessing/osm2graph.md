# Osm2graph

## What is osm2graph?
Osm2graph is a tool which helps to convert a osm file to an edge-based graph representation and to add useful information to the edges of this graph. It is ment to be used in combination with osmosis (see 'also useful').
Amongst others, it allows to add to each edge:
 - information of nearby points of interest
 - whether is passes though certain environments
 - whether the edge is near to other way osm objects (e.g., whether the edge is near to water, near to a primary way,...)
 - information of the relations that contain the edge
 - information of neighbouring edges, which share a node
 
Osm2graph operates by reading an input file one or more times. Each reading is called a 'step'. While scanning through the document, a stream of information is produced. This stream is processed by serveral stream processors, which can be specified through options. Each processor adapts the stream and passes it to the next processor, thus allowing to chain processors.

Some of the processors adapt the stream from their first read on. These processors are called 'writers'. Other processors, 'read-writers', first need to read through the entire stream, and start adapting the stream from their second read on. As soon as a processor starts to adapt the stream, it also adapts the stream in each subsequent read, thus ensuring that subsequent processors get the same input in each step.

## Dependencies
 - Python 3, including pip. If you are a Windows user, do not forget to add both the python installation dir and the 'Scripts' subdir to your path

 - Following pip packages are always required:
     - haversine
     - shapely ([Windows](http://www.lfd.uci.edu/~gohlke/pythonlibs/#shapely), [Linux](https://pypi.python.org/pypi/Shapely))
     - rtree ([Windows](https://github.ugent.be/pkstroob/bike-routing/blob/master/preprocessing/RtreeWindows.md), [Linux](http://toblerity.org/rtree/install.html#nix))

 - If you are not interested to calculate height differences you should remove `height.py` from the 'extra' subfolder. If you are interested, following pip packages are required:
     - GDAL ([Windows](http://www.lfd.uci.edu/~gohlke/pythonlibs/#gdal), [Linux](http://www.sarasafavi.com/installing-gdalogr-on-ubuntu.html))
	 
## Options
Options can be passed through their short form (only a '-', followed by the short name) or by their long form (a '--', followed by the long name). Some of the options require extra arguments, a suboption name and value are separated by an '=' sign. E.g.: 
```sh
$ pyton osm2graph --cross endings remap=highway.primary.nrPrimaryNeighbours -st -xml out=out.osm in.osm
```
This command executes the 'cross' option with the argument endings (which requires no value) and the argument remap, with value highway.primary.nrPrimaryNeighbours. The 'cross' option specifies a read-writer, so the effect of this option will only be visible in the stream during the next step. The -st option ensures that subsequent options are executed in the next step and the -xml option with extra argument 'out' writes the output to out.osm. The last argument is always the input file.

### Common
 - -v, --verbose: Print timing info and in which step the algorithm is.
 - -st, --step: Increment the step in which the options following this command are started.

### Read-Writers
 - -cr, --cross: Count how many neighbouring edges contain given tags. 
   - remap: A comma separated list of the tags which must be checked, their value and the new tag on which the number of tag-values must be mapped. If no value is specified, or a * is passed, all tags with a matching key are remapped. If no new tag is specified, tags are remapped onto themselves. E.g.: remap=highway.primary.nrPrimaryNeighbours,highway.*.nrNeighbours
   - endings: Optional. Check only for neighbouring ways at the start and end points of a way. This brings benefits in memry and performance and is useful when the ways are split up into edges.
 - -inh, --inherit: Count how many relations with given tags contain a way.
   - remap: A comma separated list of the tags which must be checked, their value and the new tag on which the number of tag-values must be mapped. If no value is specified, or a * is passed, all tags with a matching key are remapped. If no new tag is specified, tags are remapped onto themselves. E.g.: remap=route.bicycle.nrBikeroutes,route.foot will result in a tag nrBikeroutes containing the number of relations with tags with key route and value bike that pass through a way and a tag foot containing the number of relations with tags with key route and value foot that pass through a way.
 - -pt, --pass-through: Detect through how many of the closed ways with specified tags in a given file each way in the stream passes.
   - rel: Optional. Detect whether the ways passes through the relations specified in the files, rather than the ways (relations can have inner and outer bounds).
   - in: An osm-file containing the ways (and relations) to which ways in the stream are compared.
   - remap: A comma separated list of the tags which must be checked, their value and the new tag on which the number of tag-values must be mapped. If no value is specified, or a * is passed, all tags with a matching key are remapped. If no new tag is specified, tags are remapped onto themselves. E.g.: remap=landuse.forest.nrForests,natural.wood
 - -poi, --poi: Detect for each node and closed way from a given file which way from the stream is the closest and count for each way of the stream how many of these points of interest tags are assigned to it.
   - in: An osm-file containing the nodes and ways to which ways in the stream are compared.
   - remap: A comma separated list of the tags which must be checked, their value and the new tag on which the number of tag-values must be mapped. If no value is specified, or a * is passed, all tags with a matching key are remapped. If no new tag is specified, tags are remapped onto themselves. E.g.: remap=amenity.bar.nrBars,amenity.cafe
 - -we, --way-to-edge: Convert ways into edges by splitting them at nodes at which multiple ways intersect. The original way id is stored in the new tag wayId.
 - -pn, --pass-near: Detect whether a way runs near any way of a given file.
   - in: An osm-file containing the ways to which ways in the stream are compared.
   - tag: The tag key that has to be assigned to ways that pass near to ways of the given file. The tag value for these ways is 1.

### Writers
 - -ei, --edge-info: Add the startNode, endNode and length attributes to each edge.
 - -filter, --filter: Filter out elements from the stream
   - rules: A comma-separated list, specifying rules about elements should be passed or filtered out. Each rule starts with either + or -, in which + allows an element to pass and - filters out an element (and all subelements). A subelement b of an element a is adressed as a.b, * is a wildcard for all subelements. No rule earlier in the list should be an exception of a rule later in the list. E.g.: rules=+*,-osm.node,-osm.way.attr.version means: let all elements pass, except for the osm nodes and the version attribute of the osm ways.
 - -json, --json: Write the output to a JSON-file.", [("out","The file to which the output is writen.
 - -xml, --xml: Write the output to an XML-file.", [("out","The file to which the output is writen.

## Note
It may be attractive to process a file by executing osm2graph multiple times, but we recommend using as few commands as possible. When different options in the same or subsequent step require similar information, osm2graph will not recalculate this information, but will instead reuse it, thus saving both memory and computation time.
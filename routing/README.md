# Routing
## Requirements
The code required for routing is written in Java. The dependencies are managed through Maven, so importing the src/main project as a maven project in your favourite java-editor will resolve all dependencies and allow you to edit and execute the code.
Alternatively, the full functionality is also available through the command line interface of the routing.jar file.

To keep the java virtual machine from spending too much time on garbage collection, we recommend to use the '-Xmx' java virtual machine option to increase the maximal allowed heap size.
For the Belgian dataset, we recommend to allow the jvm to use 5 GB (-Xmx5G), for the French dataset, we recommend to allow up to 7GB of memory usage (-Xmx7G).

## Finializing the preprocessing
After preprocessing the osm data, an osm-style graph representation is obtained for [Belgium](https://www.dropbox.com/s/8dvv32p44nxjxg1/belgium_wgr.tar.gz) and [France](https://www.dropbox.com/s/fi7v3a8cmeyjcb7/france_wgr.tar.gz).
This does not finishes the preprocessing however: osm2graph extracts informaion from the osm-data and converts it into a graph representation, but this tool is not able to load the graph, nor execute any graph related algorithms.
Several steps remain to be executed:
 - Extraction of the largest strongly connected component
 - Introduction of 'shadow ways'
 - Calculation of reaches
 - Calculation of local minimal and maximal heightdifferences

In what follows, we discuss each of these steps in order. These steps can be skipped by downloading the fully preprocessed data of [Belgium](https://www.dropbox.com/s/stg8ww7uydmfnzs/belgium_fin.tar.gz) or [France](https://www.dropbox.com/s/j3rw546y4w8dyv5/france_height.tar.gz).

### Extraction of largest strongly connected component
The findLcc command allow to extract the largest connected component from the graph, using an implementation of [Tarjan's strongly connected component algorithm](https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm).
Since this algorithm is uses deep recursion, we recommend increasing the stack size of the jvm, using the -Xss512M option.
The findLcc algorithm has some required arguments:
 - in: The path of the file containing the input graph representation.
 - out: The path of the file that will contain the output graph representation. The output file may not overwrite the input file.

Example:
```sh
$ java -Xmx5G -Xss512M -jar routing.jar findLcc -in inputFile -out outputFile
```

### Shadow ways
Next, we introduce 'shadow ways' by applying a simple contraction, which removes all in-between nodes. The contract command also requires only an input and an output file:
 - in: The path of the file containing the input graph representation.
 - out: The path of the file that will contain the output graph representation. The output file may not overwrite the input file.

Example:
```sh
$ java -Xmx5G -jar routing.jar contract -in inputFile -out outputFile
```

### Reaches
Reaches can be calculated using the reach command. The reaches are calcuated using the total perceived path length as the weight metric and using the walking distance as the reach metric.
The following parameters are required:
 - in: The path of the file containing the input graph representation.
 - out: The path of the file that will contain the output graph representation. The output file may not overwrite the input file.
 - maxLength: the maximal walking distance for which the reach will be calculated (in m). The larger maxLength, the longer the reach calculation takes.

Optionally, following parameters can be specified:
 - wFast, wAttr, wSafe & wSimple: These weights are used for calculating the total perceived length that serves as weight metric. By default they all equal 0.25.

We also not that this command can be used to recursively calculate reaches. For example, one could start with calculating all reaches that correspond to a walking distance smaller than 5km.
By providing these reaches as an input, this command can estimate all reaches that correspond to a larger walking distance, for example 50km.

Example:
```sh
$ java -Xmx5G -jar routing.jar reach -in inputFile -out outputFile -maxLength 5000
```

### Heightdifferences
The potential for climbing (and keeping the flat ground) in the neighbourhood of each node can be calculated using the nearHeight command. This command required the following parameters:
The following parameters are required:
 - in: The path of the file containing the input graph representation.
 - out: The path of the file that will contain the output graph representation. The output file may not overwrite the input file.
 - range: the maximal walking length of the path with the maximal number of height meters and the minimal walking length of the path with the minimal number of height meters

 Optionally, following parameters can be specified:
 - wFast, wAttr, wSafe & wSimple: These weights are used for calculating the Dijkstra tree to all nodes that are within (and just outside of) the range. By default they all equal 0.25.

 Example:
```sh
$ java -Xmx5G -jar routing.jar nearHeight -in inputFile -out outputFile -range 5000
```
 
## Generating tours
Next, we discuss how to generate constrained tours.

### Length constrained tours
We can generate length constrained tours by using the findLength command. This command has several required options:
 - startId: the id of the startNode. This id corresponds to the original osm-id. The easiest way to generate a route starting from a location of one's own choosing is to look up the street via [OpenStreetMap](https://www.openstreetmap.org/). Use the search bar to find the intended way, click on the matching search result and get the id of a node that corresponds to a crossing.
 - minLength: the minimal allowed length of the generated tour
 - maxLength: the maximal allowed length of the generated tour
 - out: The path of the json-file that will contain a representation of the generated tour. This representation can be visualised using our [visualisation tools](https://github.ugent.be/pkstroob/bike-routing/tree/master/visualisation).

This command also accepts several optional arguments:
 - alt [default: 8]: The number of attempts to create an alternative tour from the given starting point.
 - wFast, wAttr, wSafe & wSimple [default: all 0.25]: The weight of the perceived fastness, attractiveness, safeness and simplicity in the total perceived weight.
 - a [default: max(0.05 maxLength, 500)]: The width of the boundary of the forward search tree in which candidate turn nodes can be found
 - b [default: 1/40]: The maximal length from the forward path to any edge that becomes poisoned by the forward path.
 - c [default: 7]: The maximal increase in total perceived edge weight due to poisoning.
 - beta [default 0.2]: The distance from the starting node at which the turn node is assumed to be (relative to the maximal length)
 - selPl [default: 1], selDist [default: 2], selConst [default: 0.01]: The weight of the pleasantness of a path and the distance from previously chosen paths when selecting a node.

There are also some parameters which are useful for creating additional visualisations or debugging:
 - prob: The name of a file which will contain the selection probability for each candidate turning node. Multiple files are generated (_0, _1, _2,...) if multiple alternative tours are generated.
 - forward: The name of a file which will contain the forward routing tree.
 - backward: The name of a file which will contain the backward routing tree. Multiple files are generated (_0, _1, _2,...) if multiple alternative tours are generated.

Example:
```sh
$ java -Xmx5G -jar routing.jar findLength -in inputFile -out outputFile.json -startId startId -minLength 45000 -maxLength 55000
```

### Length & height constrained tours
We can generate length & height constrained tours by using the findHeight command. This command has several required options:
 - startId: the id of the startNode. This id corresponds to the original osm-id. The easiest way to generate a route starting from a location of one's own choosing is to look up the street via [OpenStreetMap](https://www.openstreetmap.org/). Use the search bar to find the intended way, click on the matching search result and get the id of a node that corresponds to a crossing.
 - minLength: the minimal allowed length of the generated tour
 - maxLength: the maximal allowed length of the generated tour
 - minHeight: the minimal allowed height difference of the generated tour
 - maxHeight: the maximal allowed height difference of the generated tour
 - out: The path of the json-file that will contain a representation of the generated tour. This representation can be visualised using our [visualisation tools](https://github.ugent.be/pkstroob/bike-routing/tree/master/visualisation).

This command also accepts several optional arguments:
 - alt [default: 8]: The number of attempts to create an alternative tour from the given starting point.
 - wFast, wAttr, wSafe & wSimple [default: all 0.25]: The weight of the perceived fastness, attractiveness, safeness and simplicity in the total perceived weight.
 - b [default: 1/40]: The maximal length from the forward path to any edge that becomes poisoned by the forward path.
 - c [default: 7]: The maximal increase in total perceived edge weight due to poisoning.
 - corr [default: 10]: The maximal allowed number of attempts to correct the generated route
 - nearHeight [default: 5000]: The range that was used to calculate the potential for maximal and minimal climbing
 - dOut [default: 0.1], gammaOut [default: 0.7], dIn [default: 0.3], gammaIn [default: 3]: Used to avoid nodes that are too close to the starting node or too far from the starting node. As described in the thesis text.
 - kappa [default: 5]: Determines the effect of the pleasantness of he path to a node on the selection probability of the node, as described in the thesis text.

There are also some parameters which are useful for creating additional visualisations or debugging:
 - prob: The name of a file which will contain the selection probability for each candidate intermediary node. For each intermediate node in each of the generated tours, a file is created (end of the filename: _alternateTourNr_intermediateNodeNr).
 - constructTree: The name of a file which will contain a search tree grown from an intermediary node to find candidate nodes. For each intermediate node in each of the generated tours, a file is created (end of the filename: _alternateTourNr_intermediateNodeNr).
 - correctTree: The name of a file which will contain the forward and backward tree which are grown for finding an alternative route between two intermediary nodes. For each correction attempt, a file is created (end of the filename: _alternateTourNr_nrCorrectionAttemptsDone).
 - correctRoute: The name of a file which will contain a proposed corrected route for each correction attempt. For each correction attempt, a file is created (end of the filename: _alternateTourNr_nrCorrectionAttemptsDone).

Example:
```sh
$ java -Xmx5G -jar routing.jar findHeight -in inputFile -out outputFile.json -startId startId -minLength 95000 -maxLength 105000 -minHeight 1400 -maxHeight 2000
```
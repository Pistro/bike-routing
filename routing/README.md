# Routing
## Requirements
The code required for routing is written in Java. The dependencies are managed through Maven, so importing the src/main project as a maven project in your favourite java-editor will resolve all dependencies and allow you to edit and execute the code.
Alternatively, the full functionality is also available through the command line interface of the routing.jar file.

To keep the java virtual machine from spending too much time on garbage collection, we recommend to use the '-Xmx' java virtual machine option to increase the maximal allowed heap size.
The required heap size depends on the size of the dataset. For the French dataset, we recommend to allow up to 7GB of memory usage (-Xmx7G).

## Finalizing the preprocessing
After preprocessing the osm data, an osm-style graph representation is obtained for your region of interest.
This does not finishes the preprocessing however: osm2graph extracts informaion from the osm-data and converts it into a graph representation, but this tool is not able to load the graph, nor execute any graph related algorithms.
Several steps remain to be executed:
 - Extraction of the largest strongly connected component
 - Introduction of 'shadow ways'
 - Calculation of reaches

### Extraction of largest strongly connected component
The findLcc command allow to extract the largest connected component from the graph, using an implementation of [Tarjan's strongly connected component algorithm](https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm).
Since this algorithm is uses deep recursion, we recommend increasing the stack size of the jvm, using the -Xss512M option.
The findLcc algorithm has some required arguments:
 - in: The path of the file containing the input graph representation.
 - out: The path of the file that will contain the output graph representation. The output file may not overwrite the input file.

Example:
```sh
$ java -Xmx5G -Xss512M -jar routing.jar lcc -in inputFile -out outputFile
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
 - wFast [default: 0], wAttr [default: 0.5], wSafe [default: 0.5]: These weights are used for calculating the total perceived length that serves as weight metric.

Example:
```sh
$ java -Xmx5G -jar routing.jar reach -in inputFile -out outputFile -maxLength 5000
```
 
## Generating tours
Next, we discuss how to generate length-constrained tours.

### Length constrained tours
We can generate length constrained tours by using the findLength command. This command has several required options:
 - startId: the id of the startNode. This id corresponds to the original osm-id. The easiest way to generate a route starting from a location of one's own choosing is to look up the street via [OpenStreetMap](https://www.openstreetmap.org/). Use the search bar to find the intended way, click on the matching search result and get the id of a node that corresponds to a crossing.
 - minLength: the minimal allowed length of the generated tour
 - maxLength: the maximal allowed length of the generated tour
 - out: The path of the json-file that will contain a representation of the generated tour. This representation can be visualised using our [visualisation tools](../visualisation).

This command also accepts several optional arguments:
 - alt [default: 8]: The number of attempts to create an alternative tour from the given starting point.
 - wFast [default: 0], wAttr [default: 0.5] & wSafe [default: 0.5]: The weight of the perceived fastness, attractiveness and safeness in the total perceived weight.
 - beta [default 0.2]: The distance from the starting node at which the turn node is assumed to be (relative to the maximal length)
 
Example:
```sh
$ java -Xmx5G -jar routing.jar findLength -in inputFile -out outputFile.json -startId startId -minLength 45000 -maxLength 55000
```
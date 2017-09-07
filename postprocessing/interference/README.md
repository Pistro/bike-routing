# Interference
The code required for calculating the average penalty up to arbitrary accuracy is written in Java. The dependencies are managed through Maven, so importing the src/main project as a maven project in your favourite java-editor will resolve all dependencies and allow you to edit and execute the code.
Alternatively, the full functionality is also available through the command line interface of the routing.jar file.

To keep the java virtual machine from spending too much time on garbage collection, we recommend to use the '-Xmx' java virtual machine option to increase the maximal allowed heap size.
The required heap size depends on the size of the dataset. For the French dataset, we recommend to allow up to 7GB of memory usage (-Xmx7G).

## Note
This module reads an osm graph, and a batch of routing results that is created by the routing module. The osm file should allow to convert the routes in the results file to a sequance of nodes.
The higher accuracy average penalty is added to the results file and written away.
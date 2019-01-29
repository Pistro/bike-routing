# Postprocessing
This part processes the results via a jupyter notebook and outputs them through informative graphs, boxplots or files that can be visualised using the visualisation module.

Furthermore, it contains java code that allows to accurately calculate the average penalties of a batch of routing results that is created by the routing module. This code is in the interference folder.

## Interference
The code required for calculating the average penalty up to arbitrary accuracy is written in Java. The dependencies are managed through Maven, so importing the src/main project as a maven project in your favourite java-editor will resolve all dependencies and allow you to edit and execute the code.
Alternatively, the full functionality is also available through the command line interface of the routing.jar file.

To keep the java virtual machine from spending too much time on garbage collection, we recommend to use the '-Xmx' java virtual machine option to increase the maximal allowed heap size.
The required heap size depends on the size of the dataset. For the French dataset, we recommend to allow up to 7GB of memory usage (-Xmx7G).

### Note
The interference code requires an osm file that should allow to convert the routes in the results file to a sequence of nodes.
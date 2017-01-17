package routing.graph.weights;

import routing.graph.Edge;
import routing.graph.Tree;

/**
 * Interface of all classes that allow to retrieve edge costs
 * Created by Pieter on 23/11/2015.
 */
public interface WeightGetter {
    double getWeight(Edge current);
}

package routing.graph.weights;

import routing.graph.Edge;
import routing.graph.Tree;

/**
 * Created by piete on 23/11/2015.
 */
public interface WeightGetter {
    double getWeight(Edge current);
}

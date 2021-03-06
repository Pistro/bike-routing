package routing.graph.weights;

import routing.graph.Edge;

/**
 * A weight getter that returns the length of an edge
 * Created by Pieter on 18/04/2016.
 */
public class WeightLength implements WeightGetter {
    public WeightLength() {};

    public double getWeight(Edge current) {
        return current.getLength();
    }

}

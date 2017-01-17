package routing.graph.weights;

import routing.algorithms.exact.Dijkstra;
import routing.graph.Edge;
import routing.graph.Node;
import routing.graph.Path;
import routing.graph.Tree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * This class emulates changed weights of graph edges, without actually updating the graph
 * Created by Pieter on 29/11/2015.
 */
public class WeightChanger implements WeightGetter {
    private WeightGetter wg;
    private final HashMap<Edge, Double> changedWeights = new HashMap<Edge, Double>();
    public WeightChanger(WeightGetter wg) {
        this.wg = wg;
    }

    public void setEdgeWeight(Edge e, double d) {
        if (wg.getWeight(e)!=d) changedWeights.put(e, d);
    }

    public double getWeight(Edge current) {
        Double chWeight = changedWeights.get(current);
        if (chWeight!=null) return chWeight;
        else return wg.getWeight(current);
    }
}

package routing.graph.weights;

import routing.graph.Edge;

import java.util.HashMap;

/**
 * Created by piete on 3/12/2016.
 */
public class IdWeightChanger implements WeightGetter {
    private WeightGetter wg;
    private final HashMap<Integer, Double> changedWeights = new HashMap<>();
    public IdWeightChanger(WeightGetter wg) {
        this.wg = wg;
    }

    public void setEdgeWeight(Edge e, double d) {
        if (wg.getWeight(e)!=d) changedWeights.put(e.id, d);
    }

    public double getWeight(Edge current) {
        Double chWeight = changedWeights.get(current.id);
        if (chWeight!=null) return chWeight;
        else return wg.getWeight(current);
    }
}
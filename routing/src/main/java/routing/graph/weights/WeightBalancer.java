package routing.graph.weights;

import routing.graph.Edge;

/**
 * Calculate edge weight based on user defined preferences
 * Created by Pieter on 18/04/2016.
 */
public class WeightBalancer implements WeightGetter {
    private double wFast;
    private double wAttr;
    private double wSafe;
    public WeightBalancer(double wFast, double wAttr, double wSafe) {
        this.wFast = wFast;
        this.wAttr = wAttr;
        this.wSafe = wSafe;
    }

    public double getWeight(Edge current) {
        return Math.max(wFast*current.getWFast()+wAttr*current.getWAttr()+wSafe*current.getWSafe(), 0);
    }

    public double getWFast() { return wFast; }
    public double getWAttr() { return wAttr; }
    public double getWSafe() { return wSafe; }
}

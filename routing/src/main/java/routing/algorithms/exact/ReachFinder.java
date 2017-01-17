package routing.algorithms.exact;

import routing.graph.*;
import routing.graph.weights.WeightGetter;

import java.util.HashMap;

/**
 * Find reaches using all-pairs shortest path algoritms
 * Created by pieter on 29/02/2016.
 */
public class ReachFinder extends FloydWarshall {
    private static final double epsilon = 0.00001;

    private HashMap<Node, Double> reaches = new HashMap<>();
    public ReachFinder(Graph g, WeightGetter wg) {
        super(g, wg);
        HashMap<Long, Node> nodes = g.getNodes();
        int nrNodes = nodes.size();
        double [] []  mapDist = new double[nrNodes][nrNodes];
        for (int i = 0; i<nrNodes; i++) {
            Node u = reverseMapping[i];
            for (int j = i; j<nrNodes; j++) {
                Node v = reverseMapping[j];
                mapDist[i][j] = u.getDistance(v);
                mapDist[j][i] = mapDist[i][j];
            }
        }
        for (int k = 0; k<nrNodes; k++) {
            double maxLength = 0;
            for (int i = 0; i<nrNodes; i++) {
                if (lengths[i][k]>maxLength) {
                    for (int j = 0; j<nrNodes; j++) {
                        if (Math.abs(costs[i][k] + costs[k][j] - costs[i][j])<epsilon) {
                            // k is on the shortest path between i and j
                            maxLength = Math.max(maxLength, Math.min(lengths[i][k], lengths[k][j]));
                        }
                    }
                }
            }
            reaches.put(reverseMapping[k], maxLength);
        }
    }
    public HashMap<Node, Double> getReaches() {
        return reaches;
    }
}

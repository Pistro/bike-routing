package routing.algorithms.exact;

import routing.graph.*;
import routing.graph.weights.WeightGetter;

import java.util.HashMap;

/**
 * Find reaches using all-pairs shortest path algoritms
 * Created by pieter on 29/02/2016.
 */
public class ReachFinder extends FloydWarshall {

    public class ReachValues {
        public double weight;
        public double length;
        public double dist;
        private ReachValues(double weight, double length, double dist) {
            this.weight = weight;
            this.length = length;
            this.dist = dist;
        }
    }

    private HashMap<Node, ReachValues> reaches = new HashMap<>();
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
            double maxWeight = 0;
            double maxLength = 0;
            double maxDist = 0;
            for (int i = 0; i<nrNodes; i++) {
                if (lengths[i][k]>maxLength || distances[i][k]>maxWeight) {
                    for (int j = 0; j<nrNodes; j++) {
                        if (Math.abs(distances[i][k] + distances[k][j] - distances[i][j])<0.01) {
                            // k is on the shortest path between i and j
                            maxWeight = Math.max(maxWeight, Math.min(distances[i][k], distances[k][j]));
                            maxLength = Math.max(maxLength, Math.min(lengths[i][k], lengths[k][j]));
                            maxDist = Math.max(maxDist, Math.min(mapDist[i][k], mapDist[k][j]));
                        }
                    }
                }
            }
            reaches.put(reverseMapping[k], new ReachValues(maxWeight, maxLength, maxDist));
        }
    }
    public HashMap<Node, ReachValues> getReaches() {
        return reaches;
    }
}

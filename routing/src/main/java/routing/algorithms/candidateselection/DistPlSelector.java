package routing.algorithms.candidateselection;

import datastructure.IntPair;
import javafx.util.Pair;
import routing.algorithms.heuristics.DistanceCalculator;
import routing.graph.Node;
import routing.graph.SPGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Pieter on 18/04/2016.
 */
public class DistPlSelector extends CandidateSelector {
    private final Node center;

    private double [] distances;
    private double [] unpleasantnesses;

    public DistPlSelector(Node center) {
        this.center = center;
    }

    public void initialize(Collection<Candidate> candidates) {
        super.initialize(candidates);
        // All distances are zero
        distances = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) distances[i] = Double.MAX_VALUE;
        // Collect unpleasantnesses
        unpleasantnesses = new double[candidates.size()];
        int [] unpleasantnessBins = new int[101];
        int pos = 0;
        for (Candidate c : candidates) {
            if (c.length!=0) {
                double d = c.weight / c.length;
                unpleasantnesses[pos] = d;
                int binNr = (int) (d*10);
                if (binNr>=unpleasantnessBins.length) binNr = unpleasantnessBins.length-1;
                unpleasantnessBins[binNr]++;
            }
            pos++;
        }
        int sum = 0, pos10 = -1, pos50 = -1;
        for (int i = 0; i<unpleasantnessBins.length; i++) {
            sum += unpleasantnessBins[i];
            if (sum>unpleasantnesses.length/10 && pos10==-1) pos10 = i;
            if (sum>unpleasantnesses.length/2 && pos50==-1) pos50 = i;
        }
        if (pos10<100) {
            double pos10unpleasantness = pos10/10.;
            double factor = Math.log(0.25)/(1-(pos50+1.)/pos10);
            for (int i = 0; i<probabilities.length; i++) {
                unpleasantnesses[i] = Math.exp(factor*Math.min(0, (1-unpleasantnesses[i]/pos10unpleasantness)));
            }
        } else {
            for (int i = 0; i<probabilities.length; i++) unpleasantnesses[i] = 1;
        }
        // Set the probabilities
        setProbabilities();
    }


    private class MaxSum {
        public double max;
        public double sum;
        public MaxSum(double max, double sum) {
            this.max = max;
            this.sum = sum;
        }
    }

    public Candidate selectCandidate() {
        DistanceCalculator dc = new DistanceCalculator(center);
        // Select candidate using the probabilities
        Candidate selected = selectCandidateUsingProbabilities();
        if (selected==null) return selected;
        // Update distances
        Iterator<Candidate> tmp = candidates.iterator();
        for (int j = 0; j<candidates.size(); j++) {
            double d = dc.getDistance(selected.node.getNode(), tmp.next().node.getNode());
            if (d<distances[j]) distances[j] = d;
        }
        // Set the probabilities
        setProbabilities();
        return selected;
    }
    public void setProbabilities() {
        double maxDistance = 0;
        for (double d : distances) if (d>maxDistance) maxDistance = d;
        if (maxDistance == Double.MAX_VALUE) {
            for (int i = 0; i < probabilities.length; i++) {
                probabilities[i] = unpleasantnesses[i];
            }
        } else if (maxDistance==0) {
            for (int i = 0; i < probabilities.length; i++) {
                probabilities[i] = 0;
            }
        } else {
            double a = -1 / 0.5 * Math.log(1 - 0.75);
            for (int i = 0; i < probabilities.length; i++) {
                probabilities[i] = (1 - Math.exp(-a * distances[i] / maxDistance)) * unpleasantnesses[i];
            }
        }
        // Density
        DistanceCalculator dc = new DistanceCalculator(center);
        double lat200m = 500/dc.getLatScale(), lon200m = 500/dc.getLonScale();
        HashMap<IntPair, MaxSum> tileMaxSum = new HashMap<>();
        int pos = 0;
        for (Candidate c: candidates) {
            Node cNode = c.node.getNode();
            IntPair cPair = new IntPair((int) (cNode.getLat()/lat200m), (int) (cNode.getLon()/lon200m));
            MaxSum cMaxSum = tileMaxSum.computeIfAbsent(cPair, k -> new MaxSum(0., 0.));
            cMaxSum.max = Math.max(cMaxSum.max, probabilities[pos]);
            cMaxSum.sum += probabilities[pos];
            pos++;
        }
        pos = 0;
        for (Candidate c: candidates) {
            Node cNode = c.node.getNode();
            MaxSum cMaxSum = tileMaxSum.get(new IntPair((int) (cNode.getLat()/lat200m), (int) (cNode.getLon()/lon200m)));
            if (cMaxSum.sum!=0) probabilities[pos] = probabilities[pos] * cMaxSum.max/cMaxSum.sum;
            pos++;
        }
    }
}
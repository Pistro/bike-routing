package routing.graph.weights;

import routing.algorithms.heuristics.DistanceCalculator;
import routing.graph.*;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Returns weights of edges, increased with the estimated interference of a given forward path, l_max, strictness and lambda
 * Created by Pieter on 11/12/2016.
 */
public class PoisonedWeightGetter {
    private final WeightGetter wg;
    private final DistanceCalculator dc;
    private final double strictness_prod;
    private final double lambda;
    private final double t;
    private final PathPart root;
    private final double lMax;

    private class Rectangle {
        private final double latMin;
        private final double latMax;
        private final double lonMin;
        private final double lonMax;
        public Rectangle(double latMin, double lonMin, double latMax, double lonMax) {
            this.latMin = latMin;
            this.latMax = latMax;
            this.lonMin = lonMin;
            this.lonMax = lonMax;
        }
        public Rectangle(Rectangle r1, Rectangle r2) {
            this(Math.min(r1.latMin, r2.latMin), Math.min(r1.lonMin, r2.lonMin),
                    Math.max(r1.latMax, r2.latMax), Math.max(r1.lonMax, r2.lonMax));
        }
        public boolean overlaps(Rectangle r) {
            return latMax>r.latMin && r.latMax>latMin && lonMax>r.lonMin && r.lonMax>lonMin;
        }
        public double getArea() { return (latMax-latMin)*(lonMax-lonMin); }
    }

    private class Square extends Rectangle {
        private final double halfSize;
        public Square(Edge e, double d) {
            super((e.getStart().getLat()+e.getStop().getLat())/2*dc.getLatScale()-d, (e.getStart().getLon()+e.getStop().getLon())/2*dc.getLonScale()-d,
                    (e.getStart().getLat()+e.getStop().getLat())/2*dc.getLatScale()+d, (e.getStart().getLon()+e.getStop().getLon())/2*dc.getLonScale()+d);
            halfSize = d;
        }
    }

    private class PathPart {
        private final Rectangle pMinRect;
        private final Rectangle pMaxRect;
        private final PathPart left;
        private final PathPart right;
        private final ApproximateEdge e;
        private PathPart(PathPart left, PathPart right, ApproximateEdge e) {
            if (e!=null) {
                this.e = e;
                this.pMinRect = new Square(e, e.getPAvg()*strictness_prod-e.dMax);
                this.pMaxRect = new Square(e, (lMax-e.getPAvg())*strictness_prod-e.dMax);
                this.left = null;
                this.right = null;
            } else {
                this.left = left;
                this.right = right;
                this.pMinRect = new Rectangle(left.pMinRect, right.pMinRect);
                this.pMaxRect = new Rectangle(left.pMaxRect, right.pMaxRect);
                this.e = null;
            }
        }
        public double getInterference(Edge cur, Square pMinRect, Square pMaxRect) {
            if (this.pMinRect.overlaps(pMinRect) && this.pMaxRect.overlaps(pMaxRect)) {
                if (left!=null) return left.getInterference(cur, pMinRect, pMaxRect) + right.getInterference(cur, pMinRect, pMaxRect);
                else {
                    double expectedDist = Math.min(pMinRect.halfSize + e.getPAvg()*strictness_prod,
                            (lMax - e.getPAvg())*strictness_prod + pMaxRect.halfSize);
                    double expectedDistDec = expectedDist-e.dMax;
                    double dist2 = dc.getDistance2(e, cur);
                    if (dist2 < expectedDistDec * expectedDistDec && expectedDistDec>0) {
                        double dist = dc.getDistance(e, cur) + e.dMax;
                        if (dist < expectedDist) return (expectedDist - dist) / expectedDist * (e.getLength() * cur.getLength());
                    }
                }
            }
            return 0;
        }
    }

    public PoisonedWeightGetter(Path p, double lMax, WeightGetter wg, double lambda, double strictness, double t) {
        this.wg = wg;
        this.lMax = lMax;
        this.dc = new DistanceCalculator(p.getStart());
        this.strictness_prod = 2*strictness/Math.PI;
        this.lambda = lambda;
        this.t = t;
        this.root = buildInterferenceTree(contract(p));
    }

    public PathPart buildInterferenceTree(ArrayList<ApproximateEdge> edges) {
        ArrayList<PathPart> parts = new ArrayList<>(edges.size());
        for (ApproximateEdge e: edges) parts.add(new PathPart(null, null, e));
        while (parts.size()>1) {
            int bestPos = -1;
            double bestArea = Double.MAX_VALUE;
            for (int i = 0; i<parts.size()-1; i++) {
                PathPart first = parts.get(i);
                PathPart last = parts.get(i+1);
                double area = new Rectangle(first.pMinRect, last.pMinRect).getArea() + new Rectangle(first.pMaxRect, last.pMaxRect).getArea();
                if (area<bestArea) {
                    bestArea = area;
                    bestPos = i;
                }
            }
            PathPart replacePart = new PathPart(parts.get(bestPos), parts.get(bestPos+1), null);
            parts.set(bestPos, replacePart);
            parts.remove(bestPos+1);
        }
        return parts.get(0);
    }

    public ArrayList<ApproximateEdge> contract(Path p) {
        double pLength = p.getLength();
        ArrayList<ApproximateEdge> app = new ArrayList<>();
        double pMax = 0;
        Path s = new Path((Node) null);
        LinkedList<Edge> stored = s.getEdges();
        double pMin = p.getEdges().getFirst().getLength()/2;
        for (Edge e: p.getEdges()) {
            pMax += e.getLength()/2;
            stored.add(e);
            ApproximateEdge ap = new ApproximateEdge(s, pMin);
            double dExp = Math.min(pMin, pLength-pMax) * strictness_prod;
            double match = (dExp - ap.dMax) / dExp;
            if (match<t) {
                stored.pollLast();
                ap = new ApproximateEdge(s, pMin);
                app.add(ap);
                stored.clear();
                stored.add(e);
                pMin = pMax;
            }
            pMax += e.getLength()/2;
        }
        if (!stored.isEmpty()) app.add(new ApproximateEdge(s, pMin));
        return app;
    }
    public double getWeight(Edge current, double currentStopToStart) {
        double currentToStart = current.getLength()/2+currentStopToStart;
        Square currentToStartSquare = new Square(current, currentToStart*strictness_prod);
        Square endToCurrentSquare = new Square(current, -currentToStart*strictness_prod);
        double interf = root.getInterference(current, currentToStartSquare, endToCurrentSquare);
        return wg.getWeight(current) + 2*lambda*interf/lMax;
    }

    private class ApproximateEdge extends Edge {
        public final double pMin;
        public final double pMax;
        public final double dMax;

        private ApproximateEdge(Path p, double pMin) {
            super(0, p.getEdges().getFirst().getStart(), p.getEnd(), p.getLength());
            this.pMin = pMin;
            // dMax
            double dMax = 0;
            Node center = new SimpleNode((getStart().getLat()+getStop().getLat())/2, (getStart().getLon()+getStop().getLon())/2);
            for (Edge e: p.getEdges()) dMax = Math.max(dMax, dc.getDistance(e, center));
            this.dMax = dMax;
            // pMax
            this.pMax = pMin - p.getEdges().getFirst().getLength()/2 + getLength() - p.getEdges().getLast().getLength()/2;
        }
        double getPAvg() { return (pMin+pMax)/2; }

        @Override
        public double getWFast() { return 0; }

        @Override
        public double getWAttr() { return 0; }

        @Override
        public double getWSafe() { return 0; }
    }
}

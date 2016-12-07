package routing.algorithms.heuristics;

import datastructure.EdgeRaster;
import datastructure.IntPair;
import jdk.nashorn.internal.ir.debug.JSONWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import routing.IO.JsonWriter;
import routing.algorithms.candidateselection.CandidateSelector;
import routing.algorithms.exact.Dijkstra;
import routing.algorithms.candidateselection.Candidate;
import routing.graph.*;
import routing.graph.weights.*;

import java.io.IOException;
import java.util.*;

/**
 * Created by pieter on 23/11/2015.
 */
public class RouteLengthFinder {
    private final DistanceCalculator dc;
    private final SPGraph.NodePair start;
    private final EdgeRaster er;
    private final HashMap<Node, LinkedList<Edge>> nearbyNodes;
    private final static double beta = 0.6;
    private final static double epsilon = 0.00001;
    private final HashSet<Node> ends = new HashSet<>();
    private final WeightBalancer wb;
    private final CandidateSelector cs;
    private final double minLength;
    private final double maxLength;
    private final double lambda;
    private final double strictness;
    private final int nrAttempts;

    // Attributes used for measurements
    public long extractionTime;
    public long forwardTime;
    public long backwardTime = 0;
    public long poisonTime = 0;

    public RouteLengthFinder(WeightBalancer wb, Node start, CandidateSelector cs, double minLength, double maxLength, double lambda, double strictness, int nrAttempts, SPGraph hyper, EdgeRaster er) {
        long go = System.nanoTime();
        this.nearbyNodes = hyper.getSubgraphFast(start);
        long stop = System.nanoTime();
        extractionTime = (stop-go)/1000000;
        SPGraph.NodePair st = hyper.getNodePair(start, start);
        if (st==null) {
            for (Map.Entry<Node, LinkedList<Edge>> en: nearbyNodes.entrySet()) {
                SPGraph.NodePair np = (SPGraph.NodePair) en.getKey();
                if (np.s==start && np.e==start) {
                    st = np;
                    break;
                }
            }
        }
        this.start = st;
        dc = new DistanceCalculator(start);
        this.er = er;
        this.cs = cs;
        this.wb = wb;
        this.lambda = lambda;
        this.strictness = strictness;
        this.nrAttempts = nrAttempts;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public LinkedList<Path> findRoutes() {
        long go = System.nanoTime();
        ArrayList<Candidate> cands = forwardSearch();
        cs.initialize(cands);
        long stop = System.nanoTime();
        forwardTime = (stop-go)/1000000;
        //JSONObject jo = cs.toJSON();
        //JsonWriter jw = new JsonWriter(jo);
        //jw.write("nodes.json");
        LinkedList<Candidate> candidates = cs.selectCandidates(nrAttempts);
        LinkedList<Path> paths = new LinkedList<>();
        for (Candidate c: candidates) {
            Path forward = c.node.getPathFromRoot();
            go = System.nanoTime();
            WeightGetter wg = getWeightGetter(forward);
            stop = System.nanoTime();
            poisonTime += stop-go;
            go = System.nanoTime();
            Path p = closeTour(forward, wg);
            stop = System.nanoTime();
            backwardTime += stop-go;
            if (p!=null) paths.add(p);
        }
        poisonTime /= 1000000;
        backwardTime /= 1000000;
        return paths;
    }

    private ArrayList<Candidate> forwardSearch() {
        DistanceCalculator dc = new DistanceCalculator(start);
        // Forward Dijkstra (based on weight) until all nodes with a tourlength possibly smaller than maxLength are added
        Tree t = new Tree();
        HashSet<Candidate> candidates = new HashSet<>();
        Queue<Candidate> q = new PriorityQueue<>((o1, o2) -> o1.weight<o2.weight? -1 : (o1.weight>o2.weight? 1 : 0));
        q.add(new Candidate(new Tree.TreeNode(t.getRoot(), start, null), 0, 0));
        HashSet<Node> added = new HashSet<>();
        while (!q.isEmpty()) {
            Candidate cur = q.poll();
            Node curN = cur.node.getNode();
            if (((SPGraph.NodePair) curN).e == start.e) ends.add(curN);
            if (added.add(curN)) {
                LinkedList<Edge> outEdges = nearbyNodes.get(curN);
                if (outEdges==null) outEdges = curN.getOutEdges();
                if (cur.length+dc.getDistance(curN, start)+epsilon>=minLength && outEdges.size()>1) candidates.add(cur);
                for (Edge e: outEdges) {
                    Candidate cn = new Candidate(new Tree.TreeNode(cur.node, e.getStop(), e), cur.weight+wb.getWeight(e), cur.length + e.getLength());
                    LinkedList<Edge> cnOutEdges = nearbyNodes.get(cn.node.getNode());
                    if (cnOutEdges == null) cnOutEdges = cn.node.getNode().getOutEdges();
                    while (cnOutEdges.size()==1 && cn.length<=maxLength+epsilon) {
                        if (((SPGraph.NodePair) e.getStop()).e == start.e) ends.add(e.getStop());
                        e = cnOutEdges.getFirst();
                        cn = new Candidate(new Tree.TreeNode(cn.node, e.getStop(), e), cn.weight+wb.getWeight(e), cn.length + e.getLength());
                        cnOutEdges = nearbyNodes.get(cn.node.getNode());
                        if (cnOutEdges == null) cnOutEdges = cn.node.getNode().getOutEdges();
                    }
                    if (!added.contains(e.getStop())) {
                        double tourL = cn.length + dc.getDistance(e.getStop(), start);
                        if (tourL<=maxLength+epsilon) q.add(cn);
                        else if (cur.length>0) candidates.add(cur);
                    }
                }
            }
        }
        return new ArrayList<>(candidates);
    }

    public class PathInfo {
        Node penultimate;
        double length;
        double weight;
        public PathInfo(Node penultimate, double length, double weight) {
            this.penultimate = penultimate;
            this.weight = weight;
            this.length = length;
        }
    }

    public HashMap<Node, PathInfo> collectPartialPathInfo(Path p) {
        DistanceCalculator dc = new DistanceCalculator(start);
        HashMap<Node, PathInfo> stopInfo = new HashMap<>();
        double curPos = 0;
        double curWeight = 0;
        LinkedList<Edge> processedEdges = new LinkedList<>();
        for (Edge e : p.getEdges()) {
            double curL = curPos+e.getLength();
            curPos += e.getLength()/2;
            double interf = 0;
            double compPos = 0;
            for (Edge e0: processedEdges) {
                compPos += e0.getLength()/2;
                double frac = Math.min(curPos-compPos, Math.max(curL+dc.getDistance(e.getStop(), start), minLength)-curPos+compPos);
                double expectedDist = 2*frac*strictness/Math.PI;
                double dist = dc.getDistance(e, e0);
                if (dist<expectedDist) {
                    interf += (expectedDist-dist)/expectedDist*(e.getLength()*e0.getLength());
                }
                compPos += e0.getLength()/2;
            }
            curPos = curL;
            curWeight += wb.getWeight(e) + lambda*interf;
            processedEdges.add(e);
            stopInfo.put(e.getStop(), new PathInfo(e.getStart(), curPos, curWeight));
        }
        return stopInfo;
    }

    public Path closeTour(Path forwardPath, WeightGetter wg) {
        DistanceCalculator dc = new DistanceCalculator(start);
        // Create map with information of different points along the forward route
        HashMap<Node, PathInfo> stopInfo = collectPartialPathInfo(forwardPath);
        // Stops is a linked list with possible ending positions.
        // The starting node of the first edge (=forwardStop) is the currently considered stop
        LinkedList<Edge> stops = new LinkedList<>(forwardPath.getEdges());
        double forwardLength = 0;
        Node forwardStop = forwardPath.getStart();
        while (forwardLength<beta*0.5*minLength && !stops.isEmpty()) {
            Edge e = stops.poll();
            forwardLength += e.getLength();
            forwardStop = e.getStop();
        }
        // Dijkstra algorithm
        PriorityQueue<Candidate> candidates = new PriorityQueue<>((o1, o2) -> o1.weight<o2.weight? -1 : (o1.weight>o2.weight? 1 : 0));
        HashSet<Node> addedNodes = new HashSet<Node>();
        Tree tree = new Tree();
        for (Node n: ends) candidates.add(new Candidate(new Tree.TreeNode(tree.getRoot(), n, null), 0., 0.));
        double bestScore = Double.MAX_VALUE;
        Path bestPath = null;
        double maxLen = 0;
        while(!candidates.isEmpty() && !stopInfo.isEmpty()) {
            Candidate curCan = candidates.poll();
            Node curNode = curCan.node.getNode();
            if (!addedNodes.contains(curNode)) {
                addedNodes.add(curNode);
                PathInfo curStopInfo = stopInfo.remove(curNode);
                if (curStopInfo!=null) {
                    maxLen = Math.max(maxLen, curStopInfo.length+curCan.length);
                    while (!stops.isEmpty() && !stopInfo.containsKey(forwardStop)) {
                        Edge e = stops.poll();
                        forwardLength += e.getLength();
                        forwardStop = e.getStop();
                    }
                    if (minLength<=curStopInfo.length+curCan.length+epsilon && curStopInfo.length+curCan.length<=maxLength+epsilon && curCan.node.getEdgeFromParent().getStart()!=curStopInfo.penultimate) {
                        Path returnPath = curCan.node.getPathFromRoot();
                        returnPath.flipForward();
                        Path forwardPart = new Path(forwardPath);
                        forwardPart.trim(returnPath.getStart());
                        forwardPart.getEdges().addAll(returnPath.getEdges());
                        double curScore = (forwardPart.getWeight(wb) + lambda*forwardPart.getInterference(strictness))/forwardPart.getLength();
                        if (curScore<bestScore) {
                            bestScore = curScore;
                            bestPath = forwardPart;
                        }
                    }
                }
                if (curCan.length + forwardLength + dc.getDistance(curNode, forwardStop) <= maxLength+epsilon) { //
                    // Add all the neighbours of current to toAdd and their id's to considered id's
                    for (Edge e : curNode.getInEdges()) {
                        Candidate tn = new Candidate(new Tree.TreeNode(curCan.node, e.getStart(), e), curCan.weight + wg.getWeight(e), curCan.length + e.getLength());
                        while (tn.node.getNode().getInEdges().size()==1 && tn.length<=maxLength+epsilon) {
                            e = tn.node.getNode().getInEdges().getFirst();
                            tn = new Candidate(new Tree.TreeNode(tn.node, e.getStart(), e), tn.weight+wb.getWeight(e), tn.length + e.getLength());
                        }
                        if (!addedNodes.contains(e.getStart())) {
                            candidates.add(tn);
                        }
                    }
                }
            }
        }
        return bestPath;
    }

    private class ApproximateEdge extends Edge {
        public final double pMin;
        public final double pMax;
        public final double dMax;
        private ApproximateEdge(LinkedList<Edge> edges, double pMin) {
            this.pMin = pMin;
            // Start & end
            Edge first = edges.getFirst();
            Node start = first.getStart();
            setStart(start);
            Edge last = edges.getLast();
            Node stop = last.getStop();
            setStop(stop);
            // dMax
            double dMax = 0;
            Node center = new SimpleNode((start.getLat()+stop.getLat())/2, (start.getLon()+stop.getLon())/2);
            for (Edge e: edges) dMax = Math.max(dMax, dc.getDistance(e, center));
            this.dMax = dMax;
            // length
            double l = 0;
            for (Edge e: edges) l += e.getLength();
            setLength(l);
            // pMax
            this.pMax = pMin + l - first.getLength()/2 - last.getLength()/2;
        }
        double getPAvg() { return (pMin+pMax)/2; }
    }

    public LinkedList<ApproximateEdge> getApproximation(Path p) {
        double pLength = p.getLength();
        double compPos = 0;
        LinkedList<ApproximateEdge> pEdgesReduced = new LinkedList<>();
        LinkedList<Edge> stored = new LinkedList<>();
        if (p.getEdges().size()==0) {
            System.out.println("lala");
        }
        double pMin = p.getEdges().getFirst().getLength()/2;
        for (Edge e: p.getEdges()) {
            compPos += e.getLength()/2;
            stored.add(e);
            Node start = stored.getFirst().getStart();
            Node end = stored.getLast().getStop();
            Node center = new SimpleNode((start.getLat()+end.getLat())/2, (start.getLon()+end.getLon())/2);
            double dMax = 0;
            for (Edge e0: stored) dMax = Math.max(dMax, dc.getDistance(e0, center));
            double dExp = 2 * Math.min(pMin, pLength-compPos) * strictness / Math.PI;
            double match = (dExp - dMax) / dExp;
            if (match<0.5) {
                stored.pop();
                pEdgesReduced.add(new ApproximateEdge(stored, pMin));
                stored.clear();
                stored.add(e);
                pMin = compPos;
            }
            compPos += e.getLength()/2;
        }
        if (!stored.isEmpty()) {
            pEdgesReduced.add(new ApproximateEdge(stored, pMin));
        }
        return pEdgesReduced;
    }

    public WeightGetter getWeightGetter(Path p) {
        double pLength = p.getLength();
        double factor = Math.max(pLength/dc.getDistance(p.getStart(), p.getEnd()), 1.6);
        // Simplify the path by joining edges
        LinkedList<ApproximateEdge> approx = getApproximation(p);
        IdWeightChanger wc = new IdWeightChanger(wb);
        double s = 2*strictness/Math.PI;
        double f = s/(1-s);
        Set<IntPair> tiles = new HashSet<>();
        for (ApproximateEdge e: approx) {
            double d = f * Math.min(factor*dc.getDistance(e, p.getStart()) + e.getPAvg(), pLength - e.getPAvg() + factor*dc.getDistance(p.getEnd(), e)) - e.dMax;
            tiles.addAll(er.getNeighbourTiles(e, d));
        }
        Set<Edge> neighbours = er.collectTiles(tiles);
        for (Edge e0: neighbours) {
            double interf = 0;
            for (ApproximateEdge e: approx) {
                double frac = Math.min(e0.getLength()/2+factor*dc.getDistance(e0.getStop(), p.getStart())+e.getPAvg(),
                        pLength-e.getPAvg()+factor*dc.getDistance(p.getEnd(), e0.getStart())+e0.getLength()/2);
                double expectedDist = 2*frac*strictness/Math.PI;
                double dist = dc.getDistance(e, e0) + e.dMax;
                if (dist<expectedDist) {
                    interf += (expectedDist-dist)/expectedDist*(e.getLength()*e0.getLength());
                }
            }
            if (interf!=0) wc.setEdgeWeight(e0, wc.getWeight(e0) + lambda*interf);
        }
        return wc;
    }
}
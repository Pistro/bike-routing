package routing.algorithms.heuristics;

import routing.algorithms.candidateselection.*;
import routing.graph.*;
import routing.graph.weights.PoisonedWeightGetter;
import routing.graph.weights.WeightBalancer;
import sun.awt.image.ImageWatched;

import java.util.*;

/**
 * Heuristic that aims to find tours with a minimum score in time O(l_max log(l_max^2))
 * Created by Pieter on 29/09/2016.
 */

public class RouteLengthFinder {
    private final SPGraph.NodePair start;
    private final HashMap<Node, LinkedList<Edge>> nearbyNodes;
    private final double beta;
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

    public RouteLengthFinder(WeightBalancer wb, Node start, CandidateSelector cs, double minLength, double maxLength, double lambda, double strictness, double beta, int nrAttempts, SPGraph hyper) {
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
        this.cs = cs;
        this.wb = wb;
        this.lambda = lambda;
        this.strictness = strictness;
        this.beta = beta;
        this.nrAttempts = nrAttempts;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public LinkedList<Path> findRoutes() {
        long go = System.nanoTime();
        LinkedList<Candidate> cands = forwardSearch();
        long stop = System.nanoTime();
        forwardTime = (stop-go)/1000000;
        cs.initialize(cands);
        //JSONObject jo = cs.toJSON();
        //JsonWriter jw = new JsonWriter(jo);
        //jw.write("nodes.json");
        LinkedList<Candidate> candidates = cs.selectCandidates(nrAttempts);
        //for (Candidate c: cands) if (((SPGraph.NodePair) c.node.getNode()).e.getId()==833188309L) candidates.add(c);
        LinkedList<Path> paths = new LinkedList<>();
        for (Candidate c: candidates) {
            Path forward = c.node.getPathFromRoot();
            PoisonedWeightGetter wg = new PoisonedWeightGetter(forward, maxLength, wb, lambda, strictness, 0.8);
            go = System.nanoTime();
            Path p = closeTour(forward, wg);
            stop = System.nanoTime();
            backwardTime += stop-go;
            if (p!=null) paths.add(p);
        }
        backwardTime /= 1000000;
        return paths;
    }

    private LinkedList<Candidate> forwardSearch() {
        DistanceCalculator dc = new DistanceCalculator(start);
        // Forward Dijkstra (based on weight) until all nodes with a tourlength possibly smaller than maxLength are added
        Tree t = new Tree();
        LinkedList<Candidate> candidates = new LinkedList<>();
        Queue<Candidate> q = new PriorityQueue<>((o1, o2) -> o1.weight<o2.weight? -1 : (o1.weight>o2.weight? 1 : 0));
        q.add(new Candidate(new Tree.TreeNode(t.getRoot(), start, null), 0, 0));
        HashSet<Node> added = new HashSet<>();
        while (!q.isEmpty()) {
            Candidate cur = q.poll();
            Node curN = cur.node.getNode();
            if (((SPGraph.NodePair) curN).e == start.e) ends.add(curN);
            if (added.add(curN)) {
                boolean boundaryNode = false;
                LinkedList<Edge> outEdges = nearbyNodes.get(curN);
                if (outEdges==null) outEdges = curN.getOutEdges();
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
                        if (!e.getStop().hasReach() || e.getStop().getReach()+epsilon>=Math.min(cn.length, (minLength-tourL)/2)) {
                            if (tourL <= maxLength + epsilon) {
                                q.add(cn);
                            } else if (cur.length > 0) boundaryNode = true;
                        }
                    }
                }
                double minlenMinCurlen = minLength-cur.length;
                if (boundaryNode || (minlenMinCurlen<0 || dc.getDistance2(curN, start)+epsilon>=minlenMinCurlen*minlenMinCurlen)) {
                    if (outEdges.size()>1) candidates.add(cur);
                }
            }
        }
        return candidates;
    }

    private class PathInfo {
        private Node penultimate;
        private double length;
        private PathInfo(Node penultimate, double length) {
            this.penultimate = penultimate;
            this.length = length;
        }
    }

    private HashMap<Node, PathInfo> collectPartialPathInfo(Path p) {
        HashMap<Node, PathInfo> stopInfo = new HashMap<>();
        double curPos = 0;
        for (Edge e : p.getEdges()) {
            curPos += e.getLength();
            stopInfo.put(e.getStop(), new PathInfo(e.getStart(), curPos));
        }
        return stopInfo;
    }

    private Path closeTour(Path forwardPath, PoisonedWeightGetter wg) {
        DistanceCalculator dc = new DistanceCalculator(start);
        // Create map with information of different points along the forward route
        HashMap<Node, PathInfo> stopInfo = collectPartialPathInfo(forwardPath);
        // Stops is a linked list with possible ending positions.
        // The starting node of the first edge (=forwardStop) is the currently considered stop
        LinkedList<Edge> stops = new LinkedList<>(forwardPath.getEdges());
        double forwardLength = 0;
        Node forwardStop = forwardPath.getStart();
        while (forwardLength<beta*minLength/2 && !stops.isEmpty()) {
            Edge e = stops.poll();
            forwardLength += e.getLength();
            forwardStop = e.getStop();
        }
        // Dijkstra algorithm
        PriorityQueue<Candidate> candidates = new PriorityQueue<>((o1, o2) -> o1.weight<o2.weight? -1 : (o1.weight>o2.weight? 1 : 0));
        HashSet<Node> addedNodes = new HashSet<>();
        Tree tree = new Tree();
        for (Node n: ends) candidates.add(new Candidate(new Tree.TreeNode(tree.getRoot(), n, null), 0., 0.));
        double bestScore = Double.MAX_VALUE;
        Path bestPath = null;
        while(!candidates.isEmpty() && !stopInfo.isEmpty()) {
            Candidate curCan = candidates.poll();
            Node curNode = curCan.node.getNode();
            if (addedNodes.add(curNode)) {
                curCan.node.connect();
                PathInfo curStopInfo = stopInfo.remove(curNode);
                if (curStopInfo!=null) {
                    while (!stops.isEmpty() && !stopInfo.containsKey(forwardStop)) {
                        Edge e = stops.poll();
                        forwardLength += e.getLength();
                        forwardStop = e.getStop();
                    }
                    if (minLength<=curStopInfo.length+curCan.length+epsilon && curStopInfo.length+curCan.length<=maxLength+epsilon &&
                            (curCan.node.getEdgeFromParent()==null || ((SPGraph.NodePair) curCan.node.getEdgeFromParent().getStop()).e!=((SPGraph.NodePair) curStopInfo.penultimate).e)) {
                        Path returnPath = curCan.node.getPathFromRoot();
                        returnPath.flipForward();
                        Path forwardPart = new Path(forwardPath);
                        forwardPart.trim(returnPath.getStart());
                        forwardPart.getEdges().addAll(returnPath.getEdges());
                        double curWeight = forwardPart.getWeight(wb)/forwardPart.getLength();
                        if (curWeight<bestScore) {
                            double interference = new InterferenceGraph(forwardPart, strictness, 0.9).getInterference();
                            //double interference2 = forwardPart.getInterference(strictness);
                            double curScore = curWeight + lambda*interference;
                            if (curScore < bestScore) {
                                bestScore = curScore;
                                bestPath = forwardPart;
                            }
                        }
                    }
                }
                // Add all the neighbours of current to toAdd and their id's to considered id's
                for (Edge e : curNode.getInEdges()) {
                    Candidate tn = new Candidate(new Tree.TreeNode(curCan.node, e.getStart(), e), curCan.weight + wg.getWeight(e, curCan.length), curCan.length + e.getLength());
                    while (tn.node.getNode().getInEdges().size()==1 && tn.length<=maxLength+epsilon) {
                        e = tn.node.getNode().getInEdges().getFirst();
                        tn = new Candidate(new Tree.TreeNode(tn.node, e.getStart(), e), tn.weight+wg.getWeight(e, tn.length), tn.length + e.getLength());
                    }
                    if (!addedNodes.contains(e.getStart())) {
                        double tourL = tn.length + forwardLength + dc.getDistance(e.getStart(), forwardStop);
                        if (tourL<=maxLength+epsilon) candidates.add(tn);
                    }
                }
            }
        }
        if (bestPath!=null) {
            bestPath.addTag("turn_id_s", ((SPGraph.NodePair) forwardPath.getEnd()).s.getId());
            bestPath.addTag("turn_id_e", ((SPGraph.NodePair) forwardPath.getEnd()).e.getId());
        }
        return bestPath;
    }


}
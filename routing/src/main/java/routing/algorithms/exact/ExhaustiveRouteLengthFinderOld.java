package routing.algorithms.exact;

import routing.algorithms.heuristics.DistanceCalculator;
import routing.graph.*;
import routing.graph.weights.WeightChanger;
import routing.graph.weights.WeightGetter;
import routing.graph.weights.WeightLength;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Branch-and-bound algorithm to find the best length constrained path.
 * Created by Pieter Stroobant in September 2016.
 */
public class ExhaustiveRouteLengthFinderOld {
    // Algorithmic parameters
    // ----------------------
    // Starting node
    private final Node start;
    // Minimal allowed tour length
    private final double minLength;
    // Maximal allowed tour length
    private final double maxLength;
    // Allows to get the weight of an edge
    private final WeightGetter wg;
    // Importance of interference
    private final double lambda;
    // Strictness for interference
    private final double strictness;
    // Hypergraph, to ensure local optimality
    private final Graph hyper;

    // Allows to take rounding errors in summation of edge lengths into account, should be smaller than the precision of edge lengths
    private final static double epsilon = 0.00001;
    // Distance calculator
    private final DistanceCalculator dc;

    private int accuracy = 25;

    // Internal variables
    // ------------------

    // Subgraph containing all relevant nodes within range of the starting node
    private Graph g = new Graph();
    private final Node hyperStart;

    private HashMap<Node, Integer> nodeToIdx = new HashMap<>();
    private final double [] returnLengths;
    private HashMap<Node, Double> hyperReturnLengths = new HashMap<>();

    private Edge [] edges;
    private int [] edgeStarts;
    private int [] edgeStops;

    // Additional configuration
    public boolean verbose = true;
    public long maxSearchTimeMs = -1;
    private double curScore = Double.MAX_VALUE;
    private int iter = 0;

    public double getScore() {
        return curScore;
    }
    public int getNrIterations() {
        return iter;
    }

    public ExhaustiveRouteLengthFinderOld(Node start, WeightGetter wg, double lambda, double strictness, double minLength, double maxLength, SPGraph hyper) {
        this.dc = new DistanceCalculator(start);
        this.hyper = hyper.getSubgraph(start, maxLength);
        Node hs = null;
        HashSet<Node> hyperEnds = new HashSet<>();
        for (Node n: this.hyper.getNodes().values()) {
            SPGraph.NodePair np = (SPGraph.NodePair) n;
            if (np.e == start) {
                if (np.s==start) hs = n;
                hyperEnds.add(np);
            }
        }
        hyperStart = hs;
        extractSubgraph(start, maxLength);
        for (Node n: g.getNodes().values()) {
            for (Edge e: n.getOutEdges()) e.scale(Math.max(Math.round(e.getLength()/accuracy), 1)*accuracy);
        }
        this.wg = wg;
        this.start = g.getNode(start.getId());
        this.lambda = lambda;
        this.strictness = strictness;
        this.minLength = minLength;
        this.maxLength = maxLength;
        // nodeToIdx
        int idx = 0;
        for (Node n: g.getNodes().values()) {
            nodeToIdx.put(n, idx++);
        }
        // Min return lengths
        returnLengths = new double[idx];
        Dijkstra dl = new Dijkstra(new WeightLength(), false);
        dl.addStartNode(this.start);
        dl.extend(Double.MAX_VALUE);
        for (Tree.TreeNodeDist tn : dl.getTree().getTreeNodesInRange(-1, Double.MAX_VALUE)) {
            returnLengths[nodeToIdx.get(tn.node.getNode())] = tn.dist;
        }
        // Min return lengths (for pairs)
        Dijkstra dl2 = new Dijkstra(new WeightLength(), false);
        for (Node n: hyperEnds) dl2.addStartNode(n);
        dl2.extend(Double.MAX_VALUE);
        for (Tree.TreeNodeDist tn : dl2.getTree().getTreeNodesInRange(-1, Double.MAX_VALUE)) {
            hyperReturnLengths.put(tn.node.getNode(), tn.dist);
        }
        // Edges
        idx = 0;
        for (Node n: g.getNodes().values()) idx += n.getOutEdges().size();
        edges = new Edge[idx];
        edgeStarts = new int[idx];
        edgeStops = new int[idx];
        idx = 0;
        for (Node n: g.getNodes().values()) for (Edge e: n.getOutEdges()) {
            edges[idx] = e;
            edgeStarts[idx] = nodeToIdx.get(e.getStart());
            edgeStops[idx] = nodeToIdx.get(e.getStop());
            idx++;
        }
    }

    private void extractSubgraph(Node start, double range) {
        HashSet<Node> nodes = new HashSet<>();
        Queue<NodeLen> q = new PriorityQueue<>((o1, o2) -> o1.l<o2.l? -1 : (o1.l>o2.l? 1 : 0));
        q.add(new NodeLen(start, 0.));
        double curLen = -1;
        while (curLen<range && !q.isEmpty()) {
            NodeLen curNl = q.poll();
            Node curNode = curNl.n;
            curLen = curNl.l;
            if (nodes.add(curNode)) {
                for (Edge e: curNode.getOutEdges()) {
                    double eTourLen = curLen+e.getLength()+dc.getDistance(e.getStop(), start);
                    if (!nodes.contains(e.getStop()) && eTourLen<range) {
                        q.add(new NodeLen(e.getStop(), curLen+e.getLength()));
                    }
                }
            }
        }
        HashSet<Long> nodeIds = new HashSet<>();
        HashSet<Integer> edgeIds = new HashSet<>();
        for (Node n: hyper.getNodes().values()) {
            nodeIds.add(((SPGraph.NodePair) n).e.getId());
            for (Edge e: n.getOutEdges()) {
                edgeIds.add(e.id);
            }
        }
        // Collect all nodes which can possibly be part of the tour
        for (Node n: nodes) {
            if (nodeIds.contains(n.getId())) {
                g.addNode(new SimpleNode((SimpleNode) n));
            }
        }
        // Build a subgraph containing only the useful nodes
        for (Node n: nodes) {
            for (Edge e : n.getOutEdges()) {
                if (edgeIds.contains(e.id)) {
                    Edge e_new = new Edge(e, g.getNode(e.getStart().getId()), g.getNode(e.getStop().getId()));
                    e_new.id = e.id;
                }
            }
        }
    }

    private ApproximatePath mergePath (ApproximatePath p, HashSet<ApproximatePath> ar){
        // Check for merge!
        double maxMatch = -Double.MAX_VALUE;
        ApproximateEdge bestAe = null;
        ApproximatePath bestP = null;
        ArrayList<Edge> pEdges = new ArrayList<>(p.getEdges());
        int bestPre = 0, bestPost = 0;
        for (ApproximatePath oPath : ar) {
            ArrayList<Edge> oEdges = new ArrayList<>(oPath.getEdges());
            // Find overlap of first parts
            int preNr = 0;
            double preLength = 0;
            while (true) {
                if (pEdges.get(preNr) == oEdges.get(preNr)) {
                    preLength += pEdges.get(preNr++).getLength();
                } else break;
            }
            // Find overlap of last parts
            int postNr = 0;
            double postLength = 0;
            while (true) {
                if (pEdges.get(pEdges.size() - 1 - postNr) == oEdges.get(oEdges.size() - 1 - postNr)) {
                    postLength += pEdges.get(pEdges.size() - 1 - postNr++).getLength();
                } else break;
            }
            // pMin and pMax
            double dStart = preLength + Math.max(pEdges.get(preNr).getLength(), oEdges.get(preNr).getLength()) / 2;
            double dEnd = postLength + Math.max(pEdges.get(pEdges.size() - 1 - postNr).getLength(), oEdges.get(oEdges.size() - 1 - postNr).getLength()) / 2;
            // Extract the different parts of both paths
            Node lastCommon = pEdges.get(preNr).getStart();
            Path eP = new Path(lastCommon, new LinkedList<>(pEdges.subList(preNr, pEdges.size() - postNr)));
            Path oP = new Path(lastCommon, new LinkedList<>(oEdges.subList(preNr, oEdges.size() - postNr)));
            // Best path
            Path bP = (eP.getWeight(wg) < oP.getWeight(wg)) ? eP : oP;
            // Approximation
            ApproximateEdge ap = new ApproximateEdge(bP, dStart - preLength, p.getLength() - dEnd - preLength);
            // dMax
            double dMax = 0;
            for (Edge e0 : eP.getEdges()) dMax = Math.max(dMax, dc.getDistance(ap, e0));
            for (Edge e0 : oP.getEdges()) dMax = Math.max(dMax, dc.getDistance(ap, e0));
            ap.dMax = dMax;
            // Scoring
            double dExp = 2 * Math.min(dStart, dEnd) * strictness / Math.PI;
            double match = (dExp - dMax) / dExp;
            if (match > maxMatch) {
                maxMatch = match;
                bestAe = ap;
                bestP = oPath;
                bestPre = preNr;
                bestPost = postNr;
            }
        }
        if (maxMatch > 0.7) {
            // Set interference in merged edges
            bestAe.inter = p.getInterference(bestPre, p.getEdges().size()-bestPost);
            // Update p
            p = new ApproximatePath(bestP);
            p.getEdges().clear();
            p.getEdges().addAll(pEdges.subList(0, bestPre));
            p.getEdges().add(bestAe);
            p.getEdges().addAll(pEdges.subList(pEdges.size() - bestPost, pEdges.size()));
            // Remove merged path from ar
            ar.remove(bestP);
        }
        return p;
    }

    private class CountDown implements Runnable {
        private long timeMs;
        private boolean running = true;
        public CountDown(long timeMs) {
            this.timeMs = timeMs;
        }

        @Override
        public void run() {
            if (timeMs>=0) {
                try {
                    Thread.sleep(timeMs);
                    running = false;
                } catch (InterruptedException ie) {
                    // Occurs when the calculation is finished before the timer expires
                }

            }
        }
    }

    public Path findRoute() {
        Integer startIdx = nodeToIdx.get(start);
        if (startIdx == null) return null;
        CountDown cntr = new CountDown(maxSearchTimeMs);
        Thread cntrThread = new Thread(cntr);
        cntrThread.start();
        PriorityQueue<PathCost> queue = new PriorityQueue<>((o1, o2) -> o1.c<o2.c? -1 : (o1.c>o2.c? 1 : 0));
        ApproximatePath startPath = new ApproximatePath(start, hyperStart);
        queue.add(new PathCost(startPath, 0));
        ApproximatePath best = null;
        double bestScore = Double.MAX_VALUE;
        iter = 0;
        HashMap<NodeLen, HashSet<ApproximatePath>> scenarios = new HashMap<>();
        HashSet<ApproximatePath> startSet = new HashSet<>();
        startSet.add(startPath);
        scenarios.put(new NodeLen(start, 0.), startSet);
        while (!queue.isEmpty()) {
            PathCost curPC = queue.poll();
            curScore = curPC.c;
            if (!cntr.running) return null;
            ApproximatePath curPath = curPC.p;
            double curLength = curPath.getLength();
            if (scenarios.get(new NodeLen(curPath.getEnd(), curLength)).remove(curPath)) {
                iter++;
                if (verbose) {
                    System.out.println(new DecimalFormat("#0.0000").format(curScore) + "/" + (bestScore>10000? "inf" : new DecimalFormat("#0.0000").format(bestScore))); // For debugging
                }
                if (curScore>=bestScore && curLength!=0) break;

                for (Edge ed : curPath.getExtensions()) {
                    ApproximatePath ePath = new ApproximatePath(curPath);
                    double eLength = curLength + ed.getLength();
                    ePath.extend(ed);
                    LinkedList<Edge> eExtensions = ePath.getExtensions();
                    while (eExtensions.size()==1 && !(((SPGraph.NodePair) ePath.hyperNode).e.getId()==start.getId() && eLength>minLength)) {
                        ePath.extend(eExtensions.getFirst());
                        eLength += eExtensions.getFirst().getLength();
                        eExtensions = ePath.getExtensions();
                    }
                    double minRetLength = hyperReturnLengths.get(ePath.hyperNode);
                    if (eLength + minRetLength < maxLength) {
                        NodeLen nl = new NodeLen(ePath.getEnd(), eLength);
                        HashSet<ApproximatePath> ar = scenarios.get(nl);
                        if (ar == null) {
                            ar = new HashSet<>();
                            scenarios.put(nl, ar);
                        }
                        ePath = mergePath(ePath, ar);
                        ar.add(ePath);

                        // Calculate the forward cost
                        double eWeight = ePath.getWeight(wg) + lambda * ePath.getInterference();
                        if (ePath.getEnd() == start && minLength < eLength && eWeight / eLength < bestScore) {
                            best = ePath;
                            bestScore = eWeight / eLength;
                        }
                        double newScore = estimateScore(ePath, eWeight, eLength);
                        /*if (newScore + 0.00001 < curScore) {// && bestMatch == null) {
                            double curWeight = curPath.getWeight(wg) + lambda * curPath.getInterference();
                            System.out.println(newScore + ">" + curScore + " wghtInc: " + (eWeight - curWeight) + " lenInc: " + (eLength - curLength));
                            estimateScore(ePath, eWeight, eLength);
                        }*/
                        if (newScore < bestScore) {
                            queue.add(new PathCost(ePath, newScore));
                        }
                    }
                }
            }
        }
        if (best == null) curScore = Double.MAX_VALUE;
        cntrThread.interrupt();
        return best;
    }

    private double estimateScore(ApproximatePath p, double pWeight, double pLength) {
        // Adapt the graph
        WeightGetter wc = p.getWeightGetter();
        // Return weights
        Dijkstra db = new Dijkstra(wc, false);
        db.addStartNode(start);
        db.extend(Double.MAX_VALUE);
        HashMap<Node, WghtLen> returnWeights = new HashMap<>();
        for (Tree.TreeNodeDist tn : db.getTree().getTreeNodesInRange(-1, Double.MAX_VALUE)) {
            Edge parentEdge = tn.node.getEdgeFromParent();
            if (parentEdge==null) {
                returnWeights.put(tn.node.getNode(), new WghtLen(0, 0));
            } else {
                WghtLen parWgtLen = returnWeights.get(parentEdge.getStop());
                returnWeights.put(tn.node.getNode(), new WghtLen(parWgtLen.w+wc.getWeight(parentEdge), parWgtLen.l+parentEdge.getLength()));
            }
        }
        double minReturnWeight = returnWeights.get(p.getEnd()).w;
        double bound1 = (minReturnWeight+pWeight)/maxLength;
        // Forward weights
        Dijkstra df = new Dijkstra(wc, true);
        df.addStartNode(start);
        df.extend(Double.MAX_VALUE);
        HashMap<Node, WghtLen> forwardWeights = new HashMap<>();
        for (Tree.TreeNodeDist tn : df.getTree().getTreeNodesInRange(-1, Double.MAX_VALUE)) {
            Edge parentEdge = tn.node.getEdgeFromParent();
            if (parentEdge==null) {
                forwardWeights.put(tn.node.getNode(), new WghtLen(0, 0));
            } else {
                WghtLen parWgtLen = forwardWeights.get(parentEdge.getStart());
                forwardWeights.put(tn.node.getNode(), new WghtLen(parWgtLen.w+wc.getWeight(parentEdge), parWgtLen.l+parentEdge.getLength()));
            }
        }
        // Naively try to find a path which satisfies
        double bound2 = Double.MAX_VALUE;
        for (Map.Entry<Node, WghtLen> en: forwardWeights.entrySet()) {
            WghtLen forward = en.getValue();
            WghtLen backward = returnWeights.get(en.getKey());
            double tourL = forward.l + backward.l, tourW = forward.w + backward.w;
            if (minLength <= tourL && tourL <= maxLength && tourL != 0) {
                bound2 = Math.min(bound2, tourW / tourL);
            }
        }
        // Slow bound
        NodeInventory inventory = new NodeInventory();
        NodeSchedule schedule = new NodeSchedule();
        inventory.addWalk(p.getEnd(), (int)pLength, pWeight);
        schedule.add(p.getEnd(), (int)pLength);
        while (!schedule.isEmpty()) {
            NodeSchedule.NodeScheduleElement nse = schedule.extractMin();
            NodeInventory.NodeToCost ntec = inventory.bestWalks[nse.len/accuracy];
            for (Map.Entry<Node, Double> en: ntec.nodeToCost.entrySet()) {
                double c = en.getValue();
                if (en.getKey()==start && minLength<=nse.len) bound2 = Math.min(bound2, c/nse.len);
                for (Edge e: en.getKey().getOutEdges()) {
                    int len = nse.len + (int) e.getLength();
                    double cost = c + wc.getWeight(e);
                    if (len + returnLengths[nodeToIdx.get(e.getStop())]<=maxLength) {
                        if ((cost + returnWeights.get(e.getStop()).w)/maxLength<bound2 && inventory.addWalk(e.getStop(), len, cost)) {
                            schedule.add(e.getStop(), len);
                        }
                    }
                }
            }
        }
        return Math.max(bound1, bound2);
    }

    private double[] forwardDistances(Node n) {
        double [] out = new double[returnLengths.length];
        for (int i = 0; i<out.length; i++) out[i] = Double.MAX_VALUE;
        Dijkstra dl = new Dijkstra(new WeightLength(), true);
        dl.addStartNode(n);
        dl.extend(Double.MAX_VALUE);
        for (Tree.TreeNodeDist tn : dl.getTree().getTreeNodesInRange(-1, Double.MAX_VALUE)) {
            out[nodeToIdx.get(tn.node.getNode())] = tn.dist;
        }
        return out;
    }

    private double estimateScoreFast(ApproximatePath p, double pWeight, double pLength) {
        // Adapt the graph
        WeightGetter wc = p.getWeightGetter();
        // Min return weight bound
        Dijkstra d = new Dijkstra(wc);
        d.addStartNode(p.getEnd());
        double minReturnWeight = d.getPath(start).getWeight(wc);
        double bound1 = (minReturnWeight+pWeight)/maxLength;
        // Knapsack bound
        double bound2 = Double.MAX_VALUE;
        double [] pEndDists = forwardDistances(p.getEnd());
        int startIdx = nodeToIdx.get(start);
        ArrayList<WghtLen> ways = new ArrayList<>();
        HashSet<Edge> addedTwins = new HashSet<>();
        for (int i = 0; i<edges.length; i++) {
            Edge e = edges[i];
            if (!addedTwins.contains(e)) {
                double retLenE = pEndDists[edgeStarts[i]]+e.getLength()+returnLengths[edgeStops[i]];
                double retLenTwin = pEndDists[edgeStops[i]]+e.getLength()+returnLengths[edgeStarts[i]];
                if (Math.min(retLenE, retLenTwin)<maxLength-pLength) {
                    Edge twin = e.getTwin();
                    if (twin!=null || retLenE<maxLength-pLength) {
                        if (twin!=null && wc.getWeight(twin)<wc.getWeight(e)) e = twin;
                        addedTwins.add(twin);
                        ways.add(new WghtLen(wc.getWeight(e), e.getLength()));
                    }
                }
            }
        }
        ArrayList<WghtLen> waysSorted = new ArrayList<>(ways);
        LinkedList<WghtLen> extraWays = new LinkedList<>();
        double minExtra = Double.MAX_VALUE;
        Collections.sort(waysSorted, (o1, o2) -> o1.cpm<o2.cpm? -1 : (o1.cpm>o2.cpm? 1 : 0));
        double minRetLen = Math.max(pEndDists[startIdx], minLength-pLength), maxRetLen = maxLength-pLength;
        double selected_length = 0, selected_weight = 0;
        for (int i=0; ; i++) {
            if (selected_length+epsilon>=minRetLen) {
                if (selected_length+epsilon<maxRetLen) {
                    bound2 = Math.min((pWeight+selected_weight)/(pLength+selected_length), bound2);
                } else {
                    bound2 = Math.min((pWeight+selected_weight-waysSorted.get(i-1).cpm*(selected_length-maxRetLen))/maxLength, bound2);
                    break;
                }
            }
            if (i==waysSorted.size() || waysSorted.get(i).cpm>=minExtra){
                if (waysSorted.isEmpty()) break;
                waysSorted.addAll(extraWays);
                Collections.sort(waysSorted, (o1, o2) -> o1.cpm<o2.cpm? -1 : (o1.cpm>o2.cpm? 1 : 0));
                minExtra = Double.MAX_VALUE;
            }
            WghtLen cur = waysSorted.get(i);
            WghtLen curNext = cur.getNext();
            extraWays.add(curNext);
            if (curNext.cpm<minExtra) minExtra = curNext.cpm;
            selected_length += cur.l;
            selected_weight += cur.w;
            if (selected_length+epsilon>=minRetLen && selected_length-cur.l+epsilon<minRetLen) {
                bound2 = Math.min((pWeight+selected_weight-cur.cpm*(selected_length-minRetLen))/minLength, bound2);
            }
        }
        return Math.max(bound1, bound2);
    }

    private class WghtLen {
        private double w;
        private double l;
        private double cpm;
        private WghtLen(double w, double l) {
            this.w = w;
            this.l = l;
            this.cpm = w/l;
        }
        private void setW(double w) {
            this.w = w;
            this.cpm = w/l;
        }
        private WghtLen getNext() {
            WghtLen out = new WghtLen(this.w, this.l);
            out.setW(w + lambda*l*l);
            return out;
        }
    }

    private class PathCost {
        private ApproximatePath p;
        private double c;
        private PathCost(ApproximatePath p, double c) {
            this.p = p;
            this.c = c;
        }
    }

    private class NodeLen {
        private Node n;
        private double l;
        private NodeLen(Node n, double l) {
            this.n = n;
            this.l = l;
        }
        @Override
        public int hashCode() {
            int hash = 1;
            hash = hash * 17 + new Double(l).hashCode();
            hash = hash * 31 + n.hashCode();
            return hash;
        }
        @Override
        public boolean equals(Object o){
            if(o == null)  return false;
            if(!(o instanceof NodeLen)) return false;
            NodeLen other = (NodeLen) o;
            return n==other.n && l==other.l;
        }
    }
    private class ApproximatePath extends Path {
        private Node hyperNode;

        private ApproximatePath(Node start, Node hyper) {
            super(start);
            this.hyperNode = hyper;
        }
        private ApproximatePath(ApproximatePath p) {
            super(p);
            this.hyperNode = p.hyperNode;
        }
        private LinkedList<Edge> getExtensions() {
            HashSet<Integer> hyperIds = new HashSet<>();
            for (Edge e: hyperNode.getOutEdges()) hyperIds.add(e.id);
            LinkedList<Edge> outs = new LinkedList<>();
            for (Edge e: getEnd().getOutEdges()) if (hyperIds.contains(e.id)) outs.add(e);
            return outs;
        }

        private double getInterference() {
            return getInterference(0, getEdges().size());
        }

        private double getInterference(int startI, int stopI) {
            double pLength = getLength();
            double interference = 0;
            double curMinPos = 0, curMaxPos = 0;
            ArrayList<Edge> pEdges = new ArrayList<Edge>(getEdges());
            double minCloseLength = returnLengths[nodeToIdx.get(getEnd())];
            for (int i = startI; i<stopI; i++) {
                Edge curEdge = pEdges.get(i);
                if (curEdge instanceof ApproximateEdge) {
                    interference += ((ApproximateEdge) curEdge).inter;
                    curMinPos += ((ApproximateEdge) curEdge).pMin;
                    curMaxPos += ((ApproximateEdge) curEdge).pMax;
                } else {
                    curMinPos += curEdge.getLength()/2;
                    curMaxPos += curEdge.getLength()/2;
                }
                // Compare current edge with predecessors
                double compMinPos = 0, compMaxPos = 0;
                for (int j=startI; j<i; j++) {
                    Edge compEdge = pEdges.get(j);
                    if (compEdge instanceof ApproximateEdge) {
                        compMinPos += ((ApproximateEdge) compEdge).pMin;
                        compMaxPos += ((ApproximateEdge) compEdge).pMax;
                    } else {
                        compMinPos += compEdge.getLength()/2;
                        compMaxPos += compEdge.getLength()/2;
                    }
                    double dist = dc.getDistance(curEdge, compEdge);
                    if (curEdge instanceof ApproximateEdge) dist += ((ApproximateEdge) curEdge).dMax;
                    if (compEdge instanceof ApproximateEdge) dist += ((ApproximateEdge) compEdge).dMax;
                    double frac = Math.min(curMinPos - compMaxPos, Math.max(pLength+minCloseLength, minLength)-curMaxPos+compMinPos);
                    double expectedDist = 2*frac*strictness/Math.PI;
                    if (compEdge instanceof ApproximateEdge) {
                        compMinPos += compEdge.getLength()-((ApproximateEdge) compEdge).pMin;
                        compMaxPos += compEdge.getLength()-((ApproximateEdge) compEdge).pMax;
                    } else {
                        compMinPos += compEdge.getLength()/2;
                        compMaxPos += compEdge.getLength()/2;
                    }
                    double inc = (expectedDist-dist)/expectedDist*(curEdge.getLength()*compEdge.getLength());
                    if (dist<expectedDist) interference += inc;
                }
                if (curEdge instanceof ApproximateEdge) {
                    curMinPos += curEdge.getLength()-((ApproximateEdge) curEdge).pMin;
                    curMaxPos += curEdge.getLength()-((ApproximateEdge) curEdge).pMax;
                } else {
                    curMinPos += curEdge.getLength()/2;
                    curMaxPos += curEdge.getLength()/2;
                }
            }
            return interference;
        }

        private void extend(Edge e) {
            getEdges().add(e);
            for (Edge e0: hyperNode.getOutEdges()) {
                if (e0.id==e.id) {
                    hyperNode = e0.getStop();
                    return;
                }
            }
            throw new IllegalArgumentException("Impossible extension");
        }

        private WeightChanger getWeightGetter() {
            double pLength = getLength();
            WeightChanger wc = new WeightChanger(wg);
            double compMinPos = 0, compMaxPos = 0;
            int startIdx = nodeToIdx.get(start);
            double [] pEndDists= forwardDistances(getEnd());
            for (Edge e: getEdges()) {
                if (e instanceof ApproximateEdge) {
                    compMinPos += ((ApproximateEdge) e).pMin;
                    compMaxPos += ((ApproximateEdge) e).pMax;
                } else {
                    compMinPos += e.getLength()/2;
                    compMaxPos += e.getLength()/2;
                }
                for (int i = 0; i<edges.length; i++) {
                    double frac = Math.min(edges[i].getLength()/2+returnLengths[startIdx]+compMinPos,
                            pLength-compMaxPos+pEndDists[edgeStarts[i]]+edges[i].getLength()/2);
                    double expectedDist = 2*frac*strictness/Math.PI;
                    double dist = dc.getDistance(e, edges[i]);
                    if (e instanceof ApproximateEdge) dist += ((ApproximateEdge) e).dMax;
                    if (dist<expectedDist) {
                        double interf = (expectedDist-dist)/expectedDist*(e.getLength()*edges[i].getLength());
                        wc.setEdgeWeight(edges[i], wc.getWeight(edges[i]) + lambda*interf);
                    }
                }
                if (e instanceof ApproximateEdge) {
                    compMinPos += e.getLength()-((ApproximateEdge) e).pMin;
                    compMaxPos += e.getLength()-((ApproximateEdge) e).pMax;
                } else {
                    compMinPos += e.getLength()/2;
                    compMaxPos += e.getLength()/2;
                }
            }
            return wc;
        }
    }
    private class ApproximateEdge extends Edge {
        private final double pMin;
        private final double pMax;
        private double dMax;
        private double inter = 0;
        private ApproximateEdge(Path bestPath, double pMin, double pMax) {
            this.pMin = pMin;
            this.pMax = pMax;
            // Set shadow
            LinkedList<Integer> sh = new LinkedList<>();
            for (Edge e: bestPath.getEdges()) {
                for (Integer i: e.shadow) sh.add(i);
            }
            this.shadow = new int[sh.size()];
            int pos = 0;
            for (Integer i: sh) this.shadow[pos++] = i;
            // Set start & end
            setStart(bestPath.getStart());
            setStop(bestPath.getEnd());
            // Sum all other attributes
            double l = 0, hd = 0, wF= 0, wA = 0, wS = 0;
            for (Edge e: bestPath.getEdges()) {
                l += e.getLength();
                hd += e.getHeightDif();
                wF += e.getWFast();
                wA += e.getWAttr();
                wS += e.getWSafe();
            }
            setLength(l);
            setHeightDif(hd);
            setWFast(wF);
            setWAttr(wA);
            setWSafe(wS);
        }
    }

    private class NodeSchedule {
        private class NodeScheduleElement {
            private int len;
            private HashSet<Node> nodes = new HashSet<>();
            private NodeScheduleElement(int len) { this.len = len; }
        }
        private NodeSchedule.NodeScheduleElement[] lenToNodes = new NodeSchedule.NodeScheduleElement[(int)Math.floor(maxLength/accuracy)+1];
        private PriorityQueue<NodeSchedule.NodeScheduleElement> queue = new PriorityQueue<>(11, (o1, o2) ->o1.len<o2.len? -1 : (o1.len>o2.len? 1 : 0));
        private void add(Node n, int l) {
            NodeSchedule.NodeScheduleElement nse = lenToNodes[l/accuracy];
            if (nse==null) {
                nse = new NodeSchedule.NodeScheduleElement(l);
                lenToNodes[l/accuracy] = nse;
                queue.add(nse);
            }
            nse.nodes.add(n);
        }
        private NodeSchedule.NodeScheduleElement extractMin() {
            NodeSchedule.NodeScheduleElement out = queue.poll();
            lenToNodes[out.len/accuracy] = null;
            return out;
        }
        private boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    private class NodeInventory {
        private class NodeToCost {
            private HashMap<Node, Double> nodeToCost = new HashMap<>();
        }
        private NodeInventory.NodeToCost[] bestWalks = new NodeInventory.NodeToCost[(int)Math.floor(maxLength/accuracy)+1];
        private boolean addWalk(Node n, int l, double c) {
            NodeInventory.NodeToCost ntec = bestWalks[l/accuracy];
            if (ntec==null) {
                ntec = new NodeInventory.NodeToCost();
                bestWalks[l/accuracy] = ntec;
            }
            Double d = ntec.nodeToCost.get(n);
            if (d == null || d>c) {
                ntec.nodeToCost.put(n, c);
                return true;
            } else return false;
        }
    }
}
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
public class ExhaustiveRouteLengthFinder {
    // Algorithmic parameters
    // ----------------------
    // Startnode (in the hypergraph)
    private final SPGraph.NodePair start;
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
    // Edge rounding accuracy
    private int accuracy = 25;

    // Internal variables
    // ------------------
    // Possible endnodes (in the hypergraph)
    private final LinkedList<Node> hyperEnds = new LinkedList<>();
    // Allows to quickly calculate great circle distance near the start node
    private final DistanceCalculator dc;

    private HashMap<Node, Integer> nodeToIdx = new HashMap<>();
    private final double [] returnLengths;

    private Edge [] edges;
    private int [] edgeStarts;
    private int [] edgeStops;

    // Additional configuration
    // ------------------------
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

    public ExhaustiveRouteLengthFinder(Node start, WeightGetter wg, double lambda, double strictness, double minLength, double maxLength, SPGraph hyper) {
        this.dc = new DistanceCalculator(start);
        this.wg = wg;
        this.lambda = lambda;
        this.strictness = strictness;
        this.minLength = minLength;
        this.maxLength = maxLength;
        Graph g = hyper.getSubgraph(start, maxLength);
        // Hyperstart & hyperends
        SPGraph.NodePair st = null;
        for (Node n: g.getNodes().values()) {
            SPGraph.NodePair np = (SPGraph.NodePair) n;
            if (np.e == start) {
                if (np.s==start) st = np;
                hyperEnds.add(n);
            }
        }
        this.start = st;
        // Round edge lengths
        for (Node n: g.getNodes().values()) {
            for (Edge e: n.getOutEdges()) e.scale(Math.max(Math.round(e.getLength()/accuracy), 1)*accuracy);
        }
        // nodeToIdx
        int idx = 0;
        for (Node n: g.getNodes().values()) {
            nodeToIdx.put(n, idx++);
        }
        // Min return lengths
        returnLengths = new double[idx];
        Dijkstra dl = new Dijkstra(new WeightLength(), false);
        for (Node n: hyperEnds) dl.addStartNode(n);
        dl.extend(Double.MAX_VALUE);
        for (Tree.TreeNodeDist tn : dl.getTree().getTreeNodesInRange(-1, Double.MAX_VALUE)) {
            returnLengths[nodeToIdx.get(tn.node.getNode())] = tn.dist;
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
            bestAe.inter = p.getUnscaledInterference(bestPre, p.getEdges().size()-bestPost);
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
        private CountDown(long timeMs) {
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
        if (start==null) return null;
        CountDown cntr = new CountDown(maxSearchTimeMs);
        Thread cntrThread = new Thread(cntr);
        cntrThread.start();
        PriorityQueue<PathCost> queue = new PriorityQueue<>((o1, o2) -> o1.c<o2.c? -1 : (o1.c>o2.c? 1 : 0));
        ApproximatePath startPath = new ApproximatePath(start);
        queue.add(new PathCost(startPath, estimateScore(startPath)));
        ApproximatePath best = null;
        double bestScore = Double.MAX_VALUE;
        iter = 0;
        HashMap<NodeLen, HashSet<ApproximatePath>> scenarios = new HashMap<>();
        HashSet<ApproximatePath> startSet = new HashSet<>();
        startSet.add(startPath);
        scenarios.put(new NodeLen(start, 0.), startSet);
        while (!queue.isEmpty()) {
            PathCost curPC = queue.poll();
            if (curPC.c>=bestScore) break;
            if (!cntr.running) return null;
            curScore = curPC.c;
            ApproximatePath curPath = curPC.p;
            double curLength = curPath.getLength();
            if (scenarios.get(new NodeLen(curPath.getEnd(), curLength)).remove(curPath)) {
                iter++;
                if (verbose) {
                    System.out.println((curScore>10000? "inf" : new DecimalFormat("#0.0000").format(curScore)) + "/" +
                            (bestScore>10000? "inf" : new DecimalFormat("#0.0000").format(bestScore)));
                }
                for (Edge ed : curPath.getEnd().getOutEdges()) {
                    ApproximatePath ePath = new ApproximatePath(curPath);
                    double eLength = curLength + ed.getLength();
                    ePath.getEdges().add(ed);
                    while (ePath.getEnd().getOutEdges().size()==1 && !(((SPGraph.NodePair) ePath.getEnd()).e==start.e && eLength>minLength)) {
                        Edge extra = ePath.getEnd().getOutEdges().getFirst();
                        ePath.getEdges().add(extra);
                        eLength += extra.getLength();
                    }
                    double minRetLength = returnLengths[nodeToIdx.get(ePath.getEnd())];

                    if (eLength + minRetLength < maxLength) {
                        NodeLen nl = new NodeLen(ePath.getEnd(), eLength);
                        HashSet<ApproximatePath> ar = scenarios.computeIfAbsent(nl, k -> new HashSet<>());
                        ePath = mergePath(ePath, ar);
                        ar.add(ePath);

                        // Calculate the forward cost
                        double eScore = ePath.getWeight(wg)/eLength + lambda*ePath.getUnscaledInterference()/(eLength*eLength);
                        if (((SPGraph.NodePair)ePath.getEnd()).e == start.e && minLength < eLength && eScore < bestScore && eLength!=0) {
                            best = ePath;
                            bestScore = eScore;
                        }
                        double newScore = estimateScore(ePath);
                        /*if (newScore + 0.00001 < curScore) {// && bestMatch == null) {
                            System.out.println(newScore + ">" + curScore);
                            LinkedList<NodeCostLen> clens = getBestEstimationPath(ePath);
                            checkBestEstimationPath(curPath, clens);
                        }*/
                        if (newScore < bestScore) {
                            queue.add(new PathCost(ePath, newScore));
                        }
                    }
                }
            }
        }
        curScore = bestScore;
        cntrThread.interrupt();
        return best;
    }

    private class WgtLen {
        private double w;
        private double l;
        private WgtLen(double w, double l) {
            this.w = w;
            this.l = l;
        }
    }

    private double estimateScore(ApproximatePath p) {
        double pWeight = p.getWeight(wg);
        double pUnscaledInterf = p.getUnscaledInterference();
        double pLength = p.getLength();
        double pMinInterfScaled = lambda*(pUnscaledInterf/maxLength)/maxLength;
        // Adapt the graph
        WeightGetter wc = p.getWeightGetter();
        // Return weights
        Dijkstra db = new Dijkstra(wc, false);
        for (Node n: hyperEnds) db.addStartNode(n);
        db.extend(Double.MAX_VALUE);
        HashMap<Node, WgtLen> returnWeights = new HashMap<>();
        for (Tree.TreeNodeDist tn : db.getTree().getTreeNodesInRange(-1, Double.MAX_VALUE)) {
            Edge parentEdge = tn.node.getEdgeFromParent();
            if (parentEdge==null) {
                returnWeights.put(tn.node.getNode(), new WgtLen(0, 0));
            } else {
                WgtLen parWgtLen = returnWeights.get(parentEdge.getStop());
                returnWeights.put(tn.node.getNode(), new WgtLen(parWgtLen.w+wc.getWeight(parentEdge), parWgtLen.l+parentEdge.getLength()));
            }
        }
        double minReturnWeight = returnWeights.get(p.getEnd()).w;
        double bound1 = (minReturnWeight+pWeight)/maxLength+pMinInterfScaled;
        // Forward weights
        Dijkstra df = new Dijkstra(wc, true);
        df.addStartNode(p.getEnd());
        df.extend(Double.MAX_VALUE);
        HashMap<Node, WgtLen> forwardWeights = new HashMap<>();
        for (Tree.TreeNodeDist tn : df.getTree().getTreeNodesInRange(-1, Double.MAX_VALUE)) {
            Edge parentEdge = tn.node.getEdgeFromParent();
            if (parentEdge==null) {
                forwardWeights.put(tn.node.getNode(), new WgtLen(0, 0));
            } else {
                WgtLen parWgtLen = forwardWeights.get(parentEdge.getStart());
                forwardWeights.put(tn.node.getNode(), new WgtLen(parWgtLen.w+wc.getWeight(parentEdge), parWgtLen.l+parentEdge.getLength()));
            }
        }
        // Naively try to find a path which satisfies
        double bound2 = Double.MAX_VALUE;
        for (Map.Entry<Node, WgtLen> en: forwardWeights.entrySet()) {
            WgtLen forward = en.getValue();
            WgtLen backward = returnWeights.get(en.getKey());
            double tourL = pLength + forward.l + backward.l, tourW = pWeight + forward.w + backward.w;
            if (minLength <= tourL && tourL <= maxLength && tourL != 0) {
                bound2 = Math.min(bound2, tourW/tourL+lambda*(pUnscaledInterf/tourL)/tourL);
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
                SPGraph.NodePair np = (SPGraph.NodePair) en.getKey();
                if (np.e==start.e && minLength<=nse.len && nse.len!=0) {
                    bound2 = Math.min(bound2, c/nse.len+lambda*(pUnscaledInterf/nse.len)/nse.len);
                }
                for (Edge e: en.getKey().getOutEdges()) {
                    int len = nse.len + (int) e.getLength();
                    double cost = c + wc.getWeight(e);
                    while (e.getStop().getOutEdges().size()==1 && !(((SPGraph.NodePair) e.getStop()).e==start.e && len>=minLength) && len<=maxLength) {
                        e = e.getStop().getOutEdges().getFirst();
                        len += (int) e.getLength();
                        cost += wc.getWeight(e);
                    }
                    if (len+returnLengths[nodeToIdx.get(e.getStop())]<=maxLength) {
                        if ((cost + returnWeights.get(e.getStop()).w)/maxLength+pMinInterfScaled<bound2 && inventory.addWalk(e.getStop(), len, cost)) {
                            schedule.add(e.getStop(), len);
                        }
                    }
                }
            }
        }
        return Math.max(bound1, bound2);
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

        private ApproximatePath(Node hyper) {
            super(hyper);
        }
        private ApproximatePath(ApproximatePath p) {
            super(p);
        }

        private double getUnscaledInterference() {
            return getUnscaledInterference(0, getEdges().size());
        }

        private double getUnscaledInterference(int startI, int stopI) {
            double pLength = getLength();
            double interference = 0;
            double curMinPos = 0, curMaxPos = 0;
            ArrayList<Edge> pEdges = new ArrayList<>(getEdges());
            double minCloseLength = returnLengths[nodeToIdx.get(getEnd())];
            for (int i = startI; i<stopI; i++) {
                Edge curEdge = pEdges.get(i);
                if (curEdge instanceof ApproximateEdge) {
                    interference += ((ApproximateEdge) curEdge).inter/2;
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
                    double dist2 = dc.getDistance2(curEdge, compEdge);
                    double frac = Math.min(curMinPos - compMaxPos, Math.max(pLength+minCloseLength, minLength)-curMaxPos+compMinPos);
                    double expectedDist = 2*frac*strictness/Math.PI;
                    double expextedDistDec = expectedDist;
                    if (curEdge instanceof ApproximateEdge) expextedDistDec -= ((ApproximateEdge) curEdge).dMax;
                    if (compEdge instanceof ApproximateEdge) expextedDistDec -= ((ApproximateEdge) compEdge).dMax;
                    if (expextedDistDec>0 && dist2<expextedDistDec*expextedDistDec) {
                        double dist = dc.getDistance(curEdge, compEdge);
                        if (curEdge instanceof ApproximateEdge) dist += ((ApproximateEdge) curEdge).dMax;
                        if (compEdge instanceof ApproximateEdge) dist += ((ApproximateEdge) compEdge).dMax;
                        if (dist<expectedDist) interference += (expectedDist-dist)/expectedDist*(curEdge.getLength()*compEdge.getLength());
                    }
                    if (compEdge instanceof ApproximateEdge) {
                        compMinPos += compEdge.getLength()-((ApproximateEdge) compEdge).pMin;
                        compMaxPos += compEdge.getLength()-((ApproximateEdge) compEdge).pMax;
                    } else {
                        compMinPos += compEdge.getLength()/2;
                        compMaxPos += compEdge.getLength()/2;
                    }
                }
                if (curEdge instanceof ApproximateEdge) {
                    curMinPos += curEdge.getLength()-((ApproximateEdge) curEdge).pMin;
                    curMaxPos += curEdge.getLength()-((ApproximateEdge) curEdge).pMax;
                } else {
                    curMinPos += curEdge.getLength()/2;
                    curMaxPos += curEdge.getLength()/2;
                }
            }
            return 2*interference;
        }

        private WeightChanger getWeightGetter() {
            double [] pEndDist = forwardDistances(getEnd());
            double [] weightIncs = new double[edges.length];
            // Calculate interferences
            double pLength = getLength();
            double compMinPos = 0, compMaxPos = 0;
            for (Edge e: getEdges()) {
                if (e instanceof ApproximateEdge) {
                    compMinPos += ((ApproximateEdge) e).pMin;
                    compMaxPos += ((ApproximateEdge) e).pMax;
                } else {
                    compMinPos += e.getLength()/2;
                    compMaxPos += e.getLength()/2;
                }
                for (int i = 0; i<edges.length; i++) {
                    Edge e0 = edges[i];
                    double frac = Math.min(e0.getLength()/2+returnLengths[edgeStops[i]]+compMinPos,
                            pLength-compMaxPos+pEndDist[edgeStarts[i]]+e0.getLength()/2);
                    double expectedDist = 2*frac*strictness/Math.PI;
                    double expectedDistDec = expectedDist;
                    double dist2 = dc.getDistance2(e, e0);
                    if (e instanceof ApproximateEdge) expectedDistDec -= ((ApproximateEdge) e).dMax;
                    if (expectedDistDec>0 && dist2<expectedDistDec*expectedDistDec) {
                        double dist = dc.getDistance(e, e0);
                        if (e instanceof ApproximateEdge) dist += ((ApproximateEdge) e).dMax;
                        if (dist<expectedDist) {
                            double interf = (expectedDist - dist) / expectedDist * (e.getLength() * e0.getLength());
                            weightIncs[i] += interf;
                        }
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
            WeightChanger wc = new WeightChanger(wg);
            for (int i = 0; i<edges.length; i++) {
                if (weightIncs[i]!=0) {
                    wc.setEdgeWeight(edges[i], wg.getWeight(edges[i])+2*lambda*weightIncs[i]/maxLength);
                }
            }
            return wc;
        }
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
            public HashSet<Node> nodes = new HashSet<>();
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
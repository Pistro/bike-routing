package routing.graph;

import org.json.simple.JSONObject;
import routing.IO.JsonWriter;
import routing.algorithms.heuristics.DistanceCalculator;
import routing.graph.weights.WeightBalancer;
import routing.graph.weights.WeightGetter;

import java.util.*;

/**
 * Created by pieter on 24/10/2016.
 */
public class SPGraph extends Graph {
    private double reach;
    private static final double epsilon = 0.00001;
    private WeightBalancer wg;
    private final GraphNodePairSet hyperNodes;
    private boolean bi;

    public double getReach() { return reach; }
    public void setReach(double reach) { this.reach = reach; }
    public WeightBalancer getWeightBalancer() { return wg; }
    public void setWeightBalancer(WeightBalancer wg) { this.wg = wg; }
    public void setBi(boolean bi) { this.bi = bi; }
    public boolean getBi() { return bi; }

    public NodePair getNodePair(Node start, Node stop) {
        return hyperNodes.getNodePair(start, stop);
    }

    private class GraphNodePairSet {
        private long idCnt = 0;
        private final Graph gr;
        private final HashMap<NodePair, NodePair> pairs = new HashMap<>();

        private GraphNodePairSet(Graph gr) {
            this.gr = gr;
        }

        private NodePair addNodePair(Node s, Node e) {
            return addNodePair(++idCnt, s, e);
        }

        private NodePair addNodePair(long id, Node s, Node e) {
            NodePair last = new NodePair(id, s, e);
            NodePair lastHyper = pairs.get(last);
            if (lastHyper==null) {
                gr.addNode(last);
                pairs.put(last, last);
                lastHyper = last;
            }
            return lastHyper;
        }

        private NodePair getNodePair(Node s, Node e) {
            return pairs.get(new NodePair(0, s, e));
        }
        private NodePair getNodePair(NodePair np) {
            return pairs.get(np);
        }
    }

    public static class NodePair extends Node {
        public Node s;
        public Node e;

        public NodePair(long id, Node s, Node e) {
            super(id);
            this.s = s;
            this.e = e;
        }
        public double getLat() { return e.getLat(); }
        public double getLon() { return e.getLon(); }
        @Override
        public int hashCode() {
            int hash = 1;
            hash = hash * 17 + s.hashCode();
            hash = hash * 31 + e.hashCode();
            return hash;
        }
        @Override
        public boolean equals(Object aThat) {
            if ( this == aThat ) return true;
            if ( !(aThat instanceof NodePair) ) return false;
            NodePair that = (NodePair)aThat;
            return s == that.s && e == that.e;
        }
    }

    public Node addNodePair(long id, Node s, Node e) {
        return hyperNodes.addNodePair(id, s, e);
    }

    public SPGraph() { hyperNodes = new GraphNodePairSet(this); }

    public SPGraph(Graph g, double reach, boolean bidirectional, WeightBalancer wb) {
        this.reach = reach;
        this.wg = wb;
        this.bi = bidirectional;

        // Construct hypergraph using every possible starting node
        hyperNodes = new GraphNodePairSet(this);
        int i = 0;
        for (Node n: g.getNodes().values()) {
            if (++i%1000==0) System.out.println(i + "/" + g.getNodes().size());
            contract(n, (treeNode, pLen) -> {
                if (pLen > reach + epsilon && pLen - treeNode.getEdgeFromParent().getLength() <= reach + epsilon) {
                    Path p = treeNode.getPathFromRoot();
                    LinkedList<Edge> pEdges = p.getEdges();
                    while (pLen > reach + epsilon) {
                        p.setStart(pEdges.getFirst().getStop());
                        pLen -= pEdges.removeFirst().getLength();
                    }
                    hyperNodes.addNodePair(p.getStart(), treeNode.getNode());
                }
            });
        }
        i = 0;
        for (Node n: g.getNodes().values()) {
            if (++i%1000==0) System.out.println(i + "/" + g.getNodes().size());
            contract(n, (treeNode, pLen) -> {
                Edge lastEdge = treeNode.getEdgeFromParent();
                if (lastEdge == null) return;
                NodePair np0 = hyperNodes.getNodePair(n, lastEdge.getStart());
                if (np0 == null) return;
                if (pLen<=reach+epsilon) {
                    NodePair np1 = hyperNodes.addNodePair(n, lastEdge.getStop());
                    addEdge(np0, np1, lastEdge);
                } else if (pLen-lastEdge.getLength()<=reach+epsilon) {
                    Path p = treeNode.getPathFromRoot();
                    LinkedList<Edge> pEdges = p.getEdges();
                    while (pLen > reach + epsilon) {
                        p.setStart(pEdges.getFirst().getStop());
                        pLen -= pEdges.removeFirst().getLength();
                    }
                    NodePair np1 = hyperNodes.getNodePair(p.getStart(), lastEdge.getStop());
                    addEdge(np0, np1, lastEdge);
                }
            });
        }
        if (!bidirectional) {
            // Resolve bidirectional edges
            for (Node cur : new LinkedList<>(getNodes().values())) {
                NodePair curNp = (NodePair) cur;
                for (Edge e : new LinkedList<>(cur.getOutEdges())) if (e.getStop() == cur) e.decouple();
                for (Edge e_in : new LinkedList<>(cur.getInEdges())) {
                    NodePair n_in = (NodePair) e_in.getStart();
                    Edge matching_out = null;
                    for (Edge e_out : cur.getOutEdges()) {
                        NodePair n_out = (NodePair) e_out.getStop();
                        if (n_in.e == n_out.e) {
                            matching_out = e_out;
                            break;
                        }
                    }
                    if (matching_out != null) {
                        Node n_new = hyperNodes.addNodePair(new SimpleNode(), curNp.e);
                        Edge e_in_new = new Edge(e_in, e_in.getStart(), n_new);
                        e_in_new.id = e_in.id;
                        for (Edge e_out: cur.getOutEdges()) {
                            if (e_out != matching_out) {
                                Edge e_new = new Edge(e_out, n_new, e_out.getStop());
                                e_new.id = e_out.id;
                            }
                        }
                        e_in.decouple();
                    }
                }
            }
        }
    }

    private interface TreeNodeProcessor {
        void process(Tree.TreeNode lastNode, double pLen);
    }

    private void contract(Node n, TreeNodeProcessor p) {
        Tree t = new Tree();
        Tree.TreeNode start = new Tree.TreeNode(t.getRoot(), n, null);
        HashMap<Node, Double> added = new HashMap<>();
        HashMap<Node, Double> proposed = new HashMap<>();
        Queue<TreeNodeCostLen> queue = new PriorityQueue<>((o1, o2) -> o1.c<o2.c? -1 : (o1.c>o2.c? 1 : 0));
        queue.add(new TreeNodeCostLen(start, 0, 0));
        double lastAddCost = 0;
        while (!queue.isEmpty()) {
            TreeNodeCostLen cur = queue.poll();
            Double curAddCost = added.get(cur.n.getNode());
            if (curAddCost==null || cur.c<curAddCost+epsilon) {
                if (curAddCost==null) {
                    added.put(cur.n.getNode(), cur.c);
                    curAddCost = cur.c;
                }
                if (proposed.isEmpty() && curAddCost>lastAddCost+epsilon) break;
                lastAddCost = curAddCost;
                proposed.remove(cur.n.getNode());
                p.process(cur.n, cur.l);
                for (Edge e: cur.n.getNode().getOutEdges()) {
                    double newCost = curAddCost+wg.getWeight(e);
                    Double newAddCost = added.get(e.getStop());
                    Double newPropCost = proposed.get(e.getStop());
                    if ((newAddCost==null || newCost<newAddCost+epsilon) && (newPropCost==null || newCost<newPropCost+epsilon)) {
                        double newLength = cur.l+e.getLength();
                        queue.add(new TreeNodeCostLen(new Tree.TreeNode(cur.n, e.getStop(), e), newCost, newLength));
                        if (cur.l<=reach+epsilon) proposed.put(e.getStop(), newCost);
                        else if (newPropCost==null || newCost+epsilon<newPropCost) proposed.remove(e.getStop());
                    }
                }
            }
        }
    }

    private class TreeNodeCostLen {
        private Tree.TreeNode n;
        private double c;
        private double l;
        private TreeNodeCostLen(Tree.TreeNode n, double c, double l) {
            this.n = n;
            this.c = l;
            this.l = l;
        }
    }

    private class NodePairLen {
        private NodePair n;
        private double l;
        private NodePairLen(NodePair n, double l) {
            this.n = n;
            this.l = l;
        }
    }

    @Override
    public Graph getSubgraph(Node start, double len) {
        DistanceCalculator dc = new DistanceCalculator(start);
        // Find starting nodes
        HashMap<NodePair, Double> forwardLens = new HashMap<>();
        HashSet<Node> createdNodes = new HashSet<>();
        contract(start, (treeNode, pLen) -> {
            Edge lastEdge = treeNode.getEdgeFromParent();
            if (pLen>reach+epsilon && pLen-lastEdge.getLength()<=reach+epsilon) {
                Path p = treeNode.getPathFromRoot();
                LinkedList<Edge> pEdges = p.getEdges();
                double pLenOrg = pLen;
                while (pLen > reach + epsilon) {
                    p.setStart(pEdges.getFirst().getStop());
                    pLen -= pEdges.removeFirst().getLength();
                }
                NodePair np = new NodePair(0, p.getStart(), treeNode.getNode());
                createdNodes.add(np);
                forwardLens.put(np, pLenOrg);
            }
        });
        // Match those staring nodes to nodes from the actual graph
        HashSet<NodePair> startPairs = new HashSet<>();
        Queue<NodePairLen> q = new PriorityQueue<>((o1, o2) -> o1.l<o2.l? -1 : (o1.l>o2.l? 1 : 0));
        for (Node n: getNodes().values()) {
            NodePair np = (NodePair) n;
            if (createdNodes.remove(np)) {
                startPairs.add((NodePair) n);
                q.add(new NodePairLen(np, forwardLens.get(np)));
            }
        }
        if (!createdNodes.isEmpty()) throw new IllegalArgumentException("Expected nodepairs are missing!");
        // Forward Dijkstra
        forwardLens.clear();
        double curLen = -1;
        while (curLen<len && !q.isEmpty()) {
            NodePairLen curNl = q.poll();
            NodePair curNode = curNl.n;
            curLen = curNl.l;
            if (!forwardLens.containsKey(curNode)) {
                forwardLens.put(curNode, curLen);
                for (Edge e: curNode.getOutEdges()) {
                    double eTourLen = curLen+e.getLength() + dc.getDistance(e.getStop(), start);
                    if (!forwardLens.containsKey(e.getStop()) && eTourLen<len) {
                        q.add(new NodePairLen((NodePair) e.getStop(), curLen+e.getLength()));
                    }
                }
            }
        }
        // Backward Dijkstra
        HashMap<NodePair, Double> backwardLens = new HashMap<>();
        q.clear();
        for (Node n: getNodes().values()) {
            NodePair np = (NodePair) n;
            if (np.e == start) {
                q.add(new NodePairLen(np, 0));
            }
        }
        curLen = -1;
        while (curLen<len && !q.isEmpty()) {
            NodePairLen curNl = q.poll();
            NodePair curNode = curNl.n;
            curLen = curNl.l;
            if (!backwardLens.containsKey(curNode)) {
                backwardLens.put(curNode, curLen);
                for (Edge e: curNode.getInEdges()) {
                    double eTourLen = curLen + e.getLength() + dc.getDistance(start, e.getStart());
                    if (!backwardLens.containsKey(e.getStart()) && eTourLen<len) {
                        q.add(new NodePairLen((NodePair) e.getStart(), curLen+e.getLength()));
                    }
                }
            }
        }
        // Extract subgraph
        Graph out = new Graph();
        GraphNodePairSet nps = new GraphNodePairSet(out);
        for (Map.Entry<NodePair, Double> nd: forwardLens.entrySet()) {
            Double backwardLen = backwardLens.get(nd.getKey());
            if (backwardLen!=null && nd.getValue()+backwardLen<=len) {
                nps.addNodePair(nd.getKey().s, nd.getKey().e);
            }
        }
        for (Node n: forwardLens.keySet()) {
            NodePair np0 = nps.getNodePair((NodePair) n);
            if (np0!=null) {
                for (Edge e: n.getOutEdges()) {
                    NodePair np1 = nps.getNodePair((NodePair) e.getStop());
                    if (np1!=null) {
                        Edge e_new = new Edge(e, np0, np1);
                        e_new.id = e.id;
                    }
                }
            }
        }
        // Add extra nodes to subgraph
        HashSet<NodePair> addedStartPairs = new HashSet<>();
        for (NodePair np: startPairs) {
            NodePair npAdded = nps.getNodePair(np.s, np.e);
            if (npAdded!=null) {
                addedStartPairs.add(np);
            }
        }
        contract(start, (treeNode, pLen) -> {
            Edge lastEdge = treeNode.getEdgeFromParent();
            if (pLen>reach+epsilon && pLen-lastEdge.getLength()<=reach+epsilon) {
                Path p = treeNode.getPathFromRoot();
                LinkedList<Edge> pEdges = p.getEdges();
                while (pLen > reach + epsilon) {
                    p.setStart(pEdges.getFirst().getStop());
                    pLen -= pEdges.removeFirst().getLength();
                }
                NodePair npf = nps.getNodePair(p.getStart(), treeNode.getNode());
                addedStartPairs.remove(npf);
                if (npf!=null) {
                    // Ensure that the entire path to npf is present
                    p = treeNode.getPathFromRoot();
                    pEdges = p.getEdges();
                    pEdges.removeLast();
                    for (Edge e: pEdges) {
                        NodePair np0 = nps.addNodePair(start, e.getStart());
                        NodePair np1 = nps.addNodePair(start, e.getStop());
                        addEdge(np0, np1, e);
                    }
                    NodePair np0 = nps.addNodePair(start, lastEdge.getStart());
                    addEdge(np0, npf, lastEdge);
                }
            }
        });
        if (!addedStartPairs.isEmpty()) throw new IllegalArgumentException("Not all startnodepairs were coupled to the graph!");

        /*HashSet<Integer> ids = new HashSet<>();
        for (Node n: out.getNodes().values()) for (Edge e: n.getOutEdges()) ids.add(e.id);
        JSONObject o = new JSONObject();
        JsonWriter w = new JsonWriter(o);
        Tree t = new Tree();
        for (Node n: g.getNodes().values()) {
            for (Edge e: n.getOutEdges()) if (ids.contains(e.id)) new Tree.TreeNode(t.getRoot(), null, e).connect();
        }
        t.writeLong(o);
        w.write("contract.json");*/
        return out;
    }

    private void addEdge(NodePair start, NodePair stop, Edge e) {
        boolean add = true;
        for (Edge e0 : start.getOutEdges()) {
            if (e0.getStop() == stop && e0.id == e.id) {
                add = false;
                break;
            }
        }
        if (add) {
            Edge e_new = new Edge(e, start, stop);
            e_new.id = e.id;
        }
    }

    public HashMap<Node, LinkedList<Edge>> getSubgraphFast(Node start) {
        HashMap<Node, LinkedList<Edge>> out = new HashMap<>();
        NodePairSet nps = new NodePairSet();
        // Forward Dijkstra
        contract(start, (treeNode, pLen) -> {
            Edge lastEdge = treeNode.getEdgeFromParent();
            if (pLen>reach+epsilon && pLen-lastEdge.getLength()<=reach+epsilon) {
                Path p = treeNode.getPathFromRoot();
                LinkedList<Edge> pEdges = p.getEdges();
                pEdges.removeLast();
                for (Edge e: pEdges) {
                    if (hyperNodes.getNodePair(start, e.getStart())==null) {
                        NodePair np0 = nps.addNodePair(start, e.getStart());
                        LinkedList<Edge> edges = out.get(np0);
                        if (edges == null) {
                            edges = new LinkedList<>();
                            out.put(np0, edges);
                        }
                        NodePair np1 = hyperNodes.getNodePair(start, e.getStop());
                        if (np1 == null) np1 = nps.addNodePair(start, e.getStop());
                        boolean add = true;
                        for (Edge e0 : edges) {
                            if (e0.getStop() == np1 && e0.id == e.id) {
                                add = false;
                                break;
                            }
                        }
                        if (add) {
                            Edge e_new = Edge.getUncoupledEdge(e, np0, np1);
                            e_new.id = e.id;
                            edges.add(e_new);
                        }
                    }
                }
                NodePair np0 = hyperNodes.getNodePair(start, lastEdge.getStart());
                if (np0 == null) {
                    np0 = nps.addNodePair(start, lastEdge.getStart());
                    pEdges.add(lastEdge);
                    while (pLen > reach + epsilon) {
                        p.setStart(pEdges.getFirst().getStop());
                        pLen -= pEdges.removeFirst().getLength();
                    }
                    NodePair np1 = hyperNodes.getNodePair(p.getStart(), lastEdge.getStop());
                    LinkedList<Edge> edges = out.get(np0);
                    if (edges == null) {
                        edges = new LinkedList<>();
                        out.put(np0, edges);
                    }
                    boolean add = true;
                    for (Edge e0 : edges) {
                        if (e0.getStop() == np1 && e0.id == lastEdge.id) {
                            add = false;
                            break;
                        }
                    }
                    if (add) {
                        Edge e_new = Edge.getUncoupledEdge(lastEdge, np0, np1);
                        e_new.id = lastEdge.id;
                        edges.add(e_new);
                    }
                }
            }
        });
        return out;
    }

    private class NodePairSet {
        private long idCnt = -1;
        public final HashMap<NodePair, NodePair> pairs = new HashMap<>();

        private NodePairSet() {}

        private NodePair addNodePair(Node s, Node e) {
            NodePair last = new NodePair(idCnt--, s, e);
            NodePair lastHyper = pairs.get(last);
            if (lastHyper==null) {
                pairs.put(last, last);
                lastHyper = last;
            }
            return lastHyper;
        }
    }
}
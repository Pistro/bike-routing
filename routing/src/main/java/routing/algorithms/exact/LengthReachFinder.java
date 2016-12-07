package routing.algorithms.exact;

import datastructure.TreeHeap;
import routing.graph.*;
import routing.graph.weights.WeightGetter;

import java.util.*;

/**
 * Created by Pieter on 1/03/2016.
 */
public class LengthReachFinder {
    private HashMap<Node, Double> distReaches = new HashMap<Node, Double>();
    private int nextNode = 0;
    private Node[] nodesToProcess;
    private HashMap<Node, Double> nodeToNbReach = new HashMap<Node, Double>();
    private Graph g = new Graph();
    private static int NR_THREADS = 16;
    private double dMax;
    private WeightGetter wg;

    public LengthReachFinder(Graph gOrg, WeightGetter wg, double dMax) {
        this.dMax = dMax;
        this.wg = wg;
        collectNodes(gOrg);
        nodesToProcess = new Node[g.getOrder()];
        int pos = 0;
        for (Node n: g.getNodes().values()) {
            nodesToProcess[pos] = n;
            pos++;
        }

        Thread[] threads = new Thread[NR_THREADS];
        for(int i = 0; i < NR_THREADS; i++) {
            threads[i] = new Thread(new reachThread());
            threads[i].start();
        }
        try {
            for (Thread thread : threads)
                thread.join();
            nodeToNbReach = null;
            for (Node n : nodesToProcess) {
                if (n.getReach()<dMax) distReaches.put(gOrg.getNode(n.getId()), n.getReach());
            }
            nodesToProcess = null;
            g = null;
        } catch (InterruptedException e) {
            System.out.println("Execution interrupted");
        }
    }

    // Build small rtree, find neighbourhoods
    public void collectNodes(Graph orgGraph) {
        for (Node n : orgGraph.getNodes().values()) {
            if (!n.hasReach()) {
                Node nNew = new SimpleNode(n.getId(), n.getLat(), n.getLon());
                double nbReach = 0;
                for (Edge e1 : n.getOutEdges()) {
                    double d = e1.getLength();
                    if (e1.getStop().hasReach() && e1.getStop().getReach() + d > nbReach)
                        nbReach = e1.getStop().getReach() + d;
                }
                for (Edge e1 : n.getInEdges()) {
                    double d = e1.getLength();
                    if (e1.getStart().hasReach() && e1.getStart().getReach() + d > nbReach)
                        nbReach = e1.getStart().getReach() + d;
                }
                nodeToNbReach.put(nNew, nbReach);
                g.addNode(nNew);
            }
        }
        for (Node n : orgGraph.getNodes().values()) {
            if (!n.hasReach()) {
                for (Edge e : n.getOutEdges()) {
                    if (!e.getStop().hasReach()) {
                        new Edge(e, g.getNode(e.getStart().getId()), g.getNode(e.getStop().getId()));
                    }
                }
            }
        }
    }

    public HashMap<Node, Double> getReaches() {
        return distReaches;
    }

    public void apply() {
        for (Map.Entry<Node, Double> en: distReaches.entrySet()) en.getKey().setReach(en.getValue());
    }

    public synchronized Node getNode() {
        if (nextNode<nodesToProcess.length) return nodesToProcess[nextNode++];
        else return null;
    }

    private class reachThread implements Runnable {
        public reachThread() {
        }

        public void run() {
            Node n = getNode();
            while (n !=null) {
                processNode(n);
                n = getNode();
            }
        }

        // Calculate distances to all nodes further than lowerbound and closer upperbound from the neighbour of each node
        public void processNode(Node n) {
            // Find the neighbour that is the most far from n
            double maxLength = 2*dMax + getFurthestNeighbour(n);
            // Now execute modified Dijkstra
            Tree t = getNearbyNodes(n, maxLength);
            // Use the resulting shortest path tree to update...
            Tree.TreeNode r = t.getRoot().getChildren().get(0);
            updateTreeNode(r, nodeToNbReach.get(r.getNode()));
        }

        public double getFurthestNeighbour(Node n) {
            double maxLength = 0;
            for (Edge e : n.getOutEdges())
                if (e.getLength() > maxLength) maxLength = e.getLength();
            return maxLength;
        }

        Tree getNearbyNodes(Node n, double maxLength) {
            Tree tree = new Tree();
            TreeHeap<Tree.TreeNode> candidates = new TreeHeap<Tree.TreeNode>();
            HashSet<Node> toReach = new HashSet<Node>();
            HashMap<Node, Double> lengthLabels = new HashMap<Node, Double>();
            HashSet<Node> addedNodes = new HashSet<Node>();
            candidates.add(n.getId(), 0.0, new Tree.TreeNode(tree.getRoot(), n, null));
            toReach.add(n);
            lengthLabels.put(n, 0.0);
            while(!toReach.isEmpty()) {
                TreeHeap.TreeHeapElement<Tree.TreeNode> curr = candidates.extractMin();
                Tree.TreeNode currTreeNode = curr.o;
                currTreeNode.connect();
                Node currNode = currTreeNode.getNode();
                toReach.remove(currNode);
                addedNodes.add(currNode);
                double currLength = lengthLabels.remove(currNode);
                double currWeight = curr.weight;
                // Add all the neighbours of current to toAdd and their Id's to considered Id's
                LinkedList<Edge> neighbours = currNode.getOutEdges();
                for (Edge e: neighbours) {
                    if (!addedNodes.contains(e.getStop())) {
                        Tree.TreeNode tentativeNode = new Tree.TreeNode(currTreeNode, e.getStop(), e);
                        double newWeight = currWeight+wg.getWeight(e);
                        double newLength = currLength+e.getLength();
                        TreeHeap.TreeHeapElement<Tree.TreeNode> replaced = candidates.add(e.getStop().getId(), newWeight, tentativeNode);
                        if (replaced==null || replaced.o!=tentativeNode) lengthLabels.put(e.getStop(), newLength);
                        if (currLength<maxLength) {
                            toReach.add(e.getStop());
                        }
                    }
                }
            }
            return tree;
        }

        public double updateTreeNode(Tree.TreeNode n, double distanceFromRoot) {
            double maxDistFromLeaf = nodeToNbReach.get(n.getNode());
            for (Tree.TreeNode child: n.getChildren()) {
                double edgeLength = child.getEdgeFromParent().getLength();
                double distFromLeaf = updateTreeNode(child, distanceFromRoot+edgeLength)+edgeLength;
                if (distFromLeaf>maxDistFromLeaf) maxDistFromLeaf = distFromLeaf;
            }
            n.getNode().setReach(Math.min(maxDistFromLeaf, distanceFromRoot));
            return maxDistFromLeaf;
        }

    }
}

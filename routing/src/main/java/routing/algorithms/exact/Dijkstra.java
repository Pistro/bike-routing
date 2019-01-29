/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.algorithms.exact;

import routing.graph.*;
import routing.graph.weights.WeightGetter;

import java.util.*;

/**
 * Dijkstra's algorithm with one or multiple beginning nodes and for source and sink trees
 * @author Pieter
 */
public class Dijkstra {
    private HashSet<Node> added = new HashSet<>();
    private final Tree tree = new Tree();
    private PriorityQueue<TreeNodeWeight> candidates = new PriorityQueue<>(Comparator.comparing(TreeNodeWeight::getWeight));
    private Tree.TreeNode lastTreeNode = null;
    private double lastDist = -1;
    private WeightGetter wg;
    private final boolean isSourceTree;

    public Dijkstra(WeightGetter wg) {
        this.wg = wg;
        this.isSourceTree = true;
    }
    public Dijkstra(WeightGetter wg, boolean isSourceTree) {
        this.wg = wg;
        this.isSourceTree = isSourceTree;
    }
    public void addStartNode(Node start) {
        candidates.add(new TreeNodeWeight(new Tree.TreeNode(tree.getRoot(), start, null), 0));
    }

    public Tree getTree() {
        return tree;
    }

    public Path getPath(Node stop) {
        Path out;
        if (!added.contains(stop)) {
            if (extend(stop)) out = lastTreeNode.getPathFromRoot();
            else return null;
        } else {
            if (lastTreeNode.getNode() == stop) out = lastTreeNode.getPathFromRoot();
            else out = tree.getPath(stop.getId());
        }
        return out;
    }

    public boolean extend(Node stop) {
        return extend(stop, Double.POSITIVE_INFINITY);
    }
    public boolean extend(double stopDist) {
        return extend(null, stopDist);
    }
    public boolean extend(Node stop, double stopDist) {
        if (!added.contains(stop)) {
            Node lastNode = null;
            while(candidates.size()!=0 && lastDist<stopDist && (stop == null || (lastNode!=stop))) {
                TreeNodeWeight lastTreeNodeWeight = candidates.poll();
                if (added.add(lastTreeNodeWeight.tn.getNode())) {
                    lastTreeNode = lastTreeNodeWeight.tn;
                    lastTreeNodeWeight.tn.connect();
                    lastNode = lastTreeNode.getNode();
                    lastDist = lastTreeNodeWeight.weight;
                    // Add all the neighbours of current to toAdd and their Id's to considered Id's
                    if (isSourceTree) {
                        LinkedList<Edge> neighbours = lastTreeNode.getNode().getOutEdges();
                        for (Edge e : neighbours) {
                            if (!added.contains(e.getStop())) {
                                Tree.TreeNode tentativeNode = new Tree.TreeNode(lastTreeNode, e.getStop(), e);
                                candidates.add(new TreeNodeWeight(tentativeNode, lastDist + wg.getWeight(e)));
                            }
                        }
                    } else {
                        LinkedList<Edge> neighbours = lastTreeNode.getNode().getInEdges();
                        for (Edge e : neighbours) {
                            if (!added.contains(e.getStart())) {
                                Tree.TreeNode tentativeNode = new Tree.TreeNode(lastTreeNode, e.getStart(), e);
                                candidates.add(new TreeNodeWeight(tentativeNode, lastDist + wg.getWeight(e)));
                            }
                        }
                    }
                }
            }
            return (lastNode == stop);
        } return true;
    }

    private class TreeNodeWeight {
        private Tree.TreeNode tn;
        private double weight;
        private TreeNodeWeight(Tree.TreeNode tn, double weight) {
            this.tn = tn;
            this.weight = weight;
        }
        private double getWeight() { return weight; }
    }
}
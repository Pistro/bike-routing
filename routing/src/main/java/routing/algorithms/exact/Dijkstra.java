/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.algorithms.exact;

import datastructure.TreeHeap;
import datastructure.TreeHeap.TreeHeapElement;
import routing.graph.*;
import routing.graph.weights.WeightGetter;

import java.util.*;

/**
 *
 * @author Pieter
 */
public class Dijkstra {
    private HashSet<Node> added = new HashSet<>();
    private final Tree tree = new Tree();
    private TreeHeap<Tree.TreeNode> candidates = new TreeHeap<>();
    private Tree.TreeNode lastAdded = null;
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
        candidates.add(start.getId(), 0.0, new Tree.TreeNode(tree.getRoot(), start, null));
    }

    public Tree getTree() {
        return tree;
    }

    public Path getPath(Node stop) {
        Path out;
        if (!added.contains(stop)) {
            if (extend(stop)) out = lastAdded.getPathFromRoot();
            else return null;
        } else {
            if (lastAdded.getNode() == stop) out = lastAdded.getPathFromRoot();
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
            Node last = null;
            while(candidates.size()!=0 && lastDist<stopDist && (stop == null || (last!=stop))) {
                TreeHeapElement<Tree.TreeNode> current = candidates.extractMin();
                current.o.connect();
                lastDist = current.weight;
                last = current.o.getNode();
                added.add(last);
                lastAdded = current.o;
                // Add all the neighbours of current to toAdd and their Id's to considered Id's
                if (isSourceTree) {
                    LinkedList<Edge> neighbours = lastAdded.getNode().getOutEdges();
                    for (Edge e: neighbours) {
                        if (!added.contains(e.getStop())) {
                            Tree.TreeNode tentativeNode = new Tree.TreeNode(lastAdded, e.getStop(), e);
                            candidates.add(e.getStop().getId(), lastDist+wg.getWeight(e), tentativeNode);
                        }
                    }
                } else {
                    LinkedList<Edge> neighbours = lastAdded.getNode().getInEdges();
                    for (Edge e: neighbours) {
                        if (!added.contains(e.getStart())) {
                            Tree.TreeNode tentativeNode = new Tree.TreeNode(lastAdded, e.getStart(), e);
                            candidates.add(e.getStart().getId(), lastDist+wg.getWeight(e), tentativeNode);
                        }
                    }
                }
            }
            return (last == stop);
        } return true;
    }
}
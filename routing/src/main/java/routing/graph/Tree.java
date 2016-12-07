package routing.graph;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 *
 * Created by Pieter on 20/11/2015.
 */
public class Tree {
    private TreeNode root;
    private HashMap<String, String> tags = new HashMap<String, String>();

    public Tree() {
        this.root = new TreeNode(null, null, null);
    }
    public Tree(Path p) {
        this.root = new TreeNode(null, null, null);
        TreeNode last = new TreeNode(root, p.getStart(), null);
        last.connect();
        for (Edge e : p.getEdges()) {
            last = new TreeNode(last, e.getStop(), e);
            last.connect();
        }
    }

    public TreeNode getRoot() {
        return root;
    }

    public Path getPath(Long stopId) {
        return root.getPathFromRoot(stopId);
    }
    public void addTag(String key, String value) { tags.put(key, value);}

    public LinkedList<TreeNodeDist> getTreeNodesInRange(double minDist, double maxDist) { // mindist <= d < maxDist
        return root.getTreeNodesInRange(new LinkedList<TreeNodeDist>(), minDist, maxDist, 0);
    }

    public LinkedList<Edge> getEdges() {
        LinkedList<Edge> edges = new LinkedList<Edge>();
        root.getEdges(edges);
        return edges;
    }

    public static class TreeNodeDist {
        public Tree.TreeNode node;
        public double dist;
        public TreeNodeDist(Tree.TreeNode node, double dist) {
            this.node = node;
            this.dist = dist;
        }
    }
    public static class TreeNode {
        private LinkedList<TreeNode> children = new LinkedList<TreeNode>();
        private TreeNode parent;
        private Edge edgeFromParent;
        private Node n;
        public LinkedList<TreeNode> getChildren() {
            return children;
        }
        public Edge getEdgeFromParent() { return edgeFromParent; }
        public TreeNode(TreeNode parent, Node n, Edge edgeFromParent) {
            this.parent = parent;
            this.edgeFromParent = edgeFromParent;
            this.n = n;
        }
        public TreeNode getParent() { return parent; }
        public void connect() {
            if (parent!=null) parent.children.add(this);
        }
        public Node getNode() {
            return n;
        }
        public Path getPathFromRoot(Long stopId) {
            if (n!=null && n.getId() == stopId) {
                return getPathFromRoot();
            }
            for (TreeNode child : children) {
                Path out = child.getPathFromRoot(stopId);
                if (out != null) return out;
            }
            return null;
        }
        public Path getPathFromRoot() {
            LinkedList<Edge> edges = new LinkedList<Edge>();
            TreeNode current = this.parent;
            TreeNode previous = this;
            while (previous.edgeFromParent!=null) {
                edges.addFirst(previous.edgeFromParent);
                previous = current;
                current = current.parent;
            }
            return new Path(previous.getNode(), edges);
        }
        private void getEdges(LinkedList<Edge> edges) {
            if (edgeFromParent!=null) edges.addLast(edgeFromParent);
            for (TreeNode child : children) {
                child.getEdges(edges);
            }
        }
        private LinkedList<TreeNode> getNodesInRange(LinkedList<TreeNode> out, double minDist, double maxDist) { // mindist <= d <= maxDist
            if (0 <= maxDist) {
                if (minDist <= 0 && n != null) out.add(this);
                for (TreeNode child : children) {
                    double length = (child.edgeFromParent!=null)? child.edgeFromParent.getLength() : 0;
                    child.getNodesInRange(out, minDist-length, maxDist-length);
                }
            }
            return out;
        }
        private LinkedList<TreeNodeDist> getTreeNodesInRange(LinkedList<TreeNodeDist> out, double minDist, double maxDist, double curDist) { // mindist <= d <= maxDist
            if (curDist <= maxDist) {
                if (minDist <= 0 && n!=null) out.add(new TreeNodeDist(this, curDist));
                for (TreeNode child : children) {
                    double length = (child.edgeFromParent!=null)? child.edgeFromParent.getLength() : 0;
                    child.getTreeNodesInRange(out, minDist, maxDist, curDist+length);
                }
            }
            return out;
        }
    }
    public void write(JSONObject j) {
        if (!j.containsKey("trees")) {
            j.put("trees", new JSONArray());
        }
        JSONArray trees = (JSONArray) j.get("trees");
        JSONObject tree = new JSONObject();
        if (!tags.isEmpty()) {
            JSONObject routeTags = new JSONObject();
            routeTags.putAll(tags);
            tree.put("tags", routeTags);
        }
        JSONArray treeWays = new JSONArray();
        HashSet<Integer> addedWays = new HashSet<>();
        LinkedList<Edge> edges = getEdges();
        for (Edge e : edges) {
            int [] shadow = e.shadow;
            for (int i=0; i<shadow.length; i++) {
                if (addedWays.add(shadow[i]))
                    treeWays.add(shadow[i]);
            }
        };
        tree.put("ways", treeWays);
        JSONArray treeStarts = new JSONArray();
        for (TreeNode r : root.getChildren()) {
            if (r.getNode()!=null) treeStarts.add(r.getNode().getId());
        }
        tree.put("starts", treeStarts);
        trees.add(tree);
    }
    public void writeLong(JSONObject j) {
        HashSet<Node> nodes = new HashSet<>();
        for (TreeNode r : root.getChildren()) {
            Edge e = r.edgeFromParent;
            if (e!=null) {
                nodes.add(e.getStart());
                nodes.add(e.getStop());
            }
        }
        // Bounds
        if (!j.containsKey("bounds")) {
            JSONObject bounds = new JSONObject();
            j.put("bounds", bounds);
            bounds.put("maxlon", Double.toString(-Double.MAX_VALUE));
            bounds.put("minlon", Double.toString(Double.MAX_VALUE));
            bounds.put("maxlat", Double.toString(-Double.MAX_VALUE));
            bounds.put("minlat", Double.toString(Double.MAX_VALUE));
        }
        JSONObject bounds = (JSONObject) j.get("bounds");
        double maxLon = Double.parseDouble((String) bounds.get("maxlon"));
        double minLon = Double.parseDouble((String) bounds.get("minlon"));
        double maxLat = Double.parseDouble((String) bounds.get("maxlat"));
        double minLat = Double.parseDouble((String) bounds.get("minlat"));
        for (Node n: nodes) {
            maxLon = Math.max(maxLon, n.getLon());
            minLon = Math.min(minLon, n.getLon());
            maxLat = Math.max(maxLat, n.getLat());
            minLat = Math.min(minLat, n.getLat());
        }
        bounds.put("maxlon", Double.toString(maxLon));
        bounds.put("minlon", Double.toString(minLon));
        bounds.put("maxlat", Double.toString(maxLat));
        bounds.put("minlat", Double.toString(minLat));
        // Nodes
        if (!j.containsKey("node")) {
            j.put("node", new JSONArray());
        }
        JSONArray node = (JSONArray) j.get("node");
        HashSet<Long> nodeIds = new HashSet<>();
        for (int i = 0; i<node.size(); i++) {
            nodeIds.add(Long.parseLong((String) ((JSONObject) node.get(i)).get("id")));
        }
        for (Node n: nodes) {
            JSONObject c = new JSONObject();
            c.put("id", Long.toString(n.getId()));
            c.put("lat", Double.toString(n.getLat()));
            c.put("lon", Double.toString(n.getLon()));
            node.add(c);
        }
        // Ways
        if (!j.containsKey("way")) {
            j.put("way", new JSONArray());
        }
        JSONArray way = (JSONArray) j.get("way");
        HashSet<Integer> wayIds = new HashSet<>();
        for (int i = 0; i<way.size(); i++) {
            wayIds.add(Integer.parseInt((String) ((JSONObject) way.get(i)).get("id")));
        }
        for (TreeNode r : root.getChildren()) {
            Edge e = r.edgeFromParent;
            if (e!=null && wayIds.add(e.id)) {
                JSONObject c = new JSONObject();
                c.put("id", Integer.toString(e.id));
                c.put("tag_start", Long.toString(e.getStart().getId()));
                c.put("tag_stop", Long.toString(e.getStop().getId()));
                JSONArray nd = new JSONArray();
                nd.add(Long.toString(e.getStart().getId()));
                nd.add(Long.toString(e.getStop().getId()));
                c.put("nd", nd);
                way.add(c);
            }
        }
    }
}

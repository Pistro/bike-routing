package routing.main.command;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.xml.sax.XMLReader;
import routing.IO.JsonWriter;
import routing.algorithms.heuristics.DistanceCalculator;
import routing.graph.Edge;
import routing.graph.Graph;
import routing.graph.Node;
import routing.graph.Path;
import routing.main.ArgParser;
import routing.main.Main;

import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * Created by Pieter on 6/06/2016.
 */
public class Interference extends Command {
    private String results;
    private String out;
    private double accuracy;
    public Interference() {}

    public Interference(ArgParser a) {
        super(a);
    }

    public boolean loadNodes() { return true; }

    public String getName() {
        return "interf";
    }

    protected void initialize(ArgParser ap) {
        results = ap.getString("res");
        out = ap.getString("out");
        accuracy = ap.getDouble("acc", 10);
    }

    private HashMap<Integer, Edge> edges = new HashMap<>();
    private Graph g;

    public void execute(Graph g) {
        this.g = g;
        try {
            System.out.println("Building edge hashmap...");
            long start = System.currentTimeMillis();
            for (Node n: g.getNodes().values()) for (Edge e: n.getOutEdges()) edges.put(e.getId(), e);
            long stop = System.currentTimeMillis();
            System.out.println("Edge hashmap built! Building time: " + 1.0 * (stop - start) / 1000 + "s");
            System.out.println("Reading result file...");
            start = System.currentTimeMillis();
            JSONObject file = (JSONObject) new JSONParser().parse(new FileReader(results));
            stop = System.currentTimeMillis();
            System.out.println("Result file read! Reading time: " + 1.0 * (stop - start) / 1000 + "s");
            System.out.println("Calculating interferences...");
            start = System.currentTimeMillis();
            JSONArray nodesJson = (JSONArray) file.get("nodes");
            for (int i = 0; i<nodesJson.size(); i++) {
                System.out.println("Processing node " + (i+1) + "/" + nodesJson.size());
                JSONObject  nodeJson = (JSONObject) nodesJson.get(i);
                Node n = g.getNode((Long) nodeJson.get("id"));
                for (Object resObj: (JSONArray) nodeJson.get("tag_results")) {
                    JSONObject resJson = (JSONObject) resObj;
                    JSONObject conf = (JSONObject) resJson.get("configuration");
                    double lamdba = (Double) conf.get("lambda");
                    double s = (Double) conf.get("strictness");
                    JSONObject outJson = (JSONObject) resJson.get("output");
                    if (outJson.containsKey("paths")) {
                        JSONArray pathsJson = (JSONArray) outJson.get("paths");
                        for (Object pathObj : pathsJson) {
                            processPath((JSONObject) pathObj, n, lamdba, s);
                        }
                    } else {
                        processPath((JSONObject) outJson.get("route"), n, lamdba, s);
                    }
                }
            }
            stop = System.currentTimeMillis();
            System.out.println("Interferences calculated! Calculation time: " + 1.0 * (stop - start) / 1000 + "s");
            System.out.println("Writing extended result file...");
            start = System.currentTimeMillis();
            new JsonWriter(file).write(out);
            stop = System.currentTimeMillis();
            System.out.println("Extended result file written! Writing time: " + 1.0 * (stop - start) / 1000 + "s");
        } catch (Exception e) {

        }
    }

    public void processPath(JSONObject pathJson, Node start, double lambda, double strictness) {
        JSONArray waysJson = (JSONArray) pathJson.get("ways");
        LinkedList<Node> nodes = new LinkedList<>();
        nodes.add(start);
        for (int i = 0; i<waysJson.size(); i++) {
            int edgeId = Math.toIntExact((Long) waysJson.get(i));
            Edge e = edges.get(edgeId);
            if (e.getStart() == nodes.getLast()) {
                for (int j = 1; j<e.shadow.length; j++) nodes.add(g.getNode(e.shadow[j]));
            } else {
                for (int j = e.shadow.length-2; j>=0; j--) nodes.add(g.getNode(e.shadow[j]));
            }
        }
        DistanceCalculator dc = new DistanceCalculator(start);
        double interference = lambda*createPath(nodes, dc).getInterference(strictness, dc);
        JSONObject tagsJson = (JSONObject) pathJson.get("tags");
        tagsJson.put("exact_interf", interference);
    }

    public Path createPath(LinkedList<Node> nodes, DistanceCalculator dc) {
        nodes = new LinkedList<>(nodes);
        nodes.add(nodes.getFirst());
        // Add in-between nodes to achieve the required accuracy
        LinkedList<Node> accuracyNodes = new LinkedList<>();
        accuracyNodes.add(nodes.getFirst());
        Iterator<Node> it = nodes.iterator();
        Node prev = it.next();
        while(it.hasNext()) {
            Node cur = it.next();
            double l = dc.getDistance(prev, cur);
            int steps = (int) Math.ceil(l/accuracy);
            for (int i = 1; i<steps; i++) {
                Node n = new Node((steps-i)*prev.getLat()/steps+i*cur.getLat()/steps, (steps-i)*prev.getLon()/steps+i*cur.getLon()/steps);
                accuracyNodes.add(n);
            }
            accuracyNodes.add(cur);
            prev = cur;
        }
        // Convert to linked list of edges
        LinkedList<Edge> edges = new LinkedList<>();
        Iterator<Node> it2 = accuracyNodes.iterator();
        prev = it2.next();
        while(it2.hasNext()) {
            Node cur = it2.next();
            edges.add(new Edge(0, prev, cur, dc.getDistance(prev, cur)));
            prev = cur;
        }
        return new Path(nodes.getFirst(), edges);
    }
}

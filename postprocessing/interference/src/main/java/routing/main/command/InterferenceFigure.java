package routing.main.command;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import routing.IO.JsonWriter;
import routing.algorithms.exact.MetricDistance;
import routing.algorithms.heuristics.DistanceCalculator;
import routing.graph.Edge;
import routing.graph.Graph;
import routing.graph.Node;
import routing.graph.Path;
import routing.main.ArgParser;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by piete on 30/08/2017.
 */
public class InterferenceFigure extends Command {
    private double accuracy;
    public InterferenceFigure() {}

    public InterferenceFigure(ArgParser a) {
        super(a);
    }

    public boolean loadNodes() { return false; }

    public String getName() {
        return "interf_figure";
    }

    protected void initialize(ArgParser ap) {
        accuracy = ap.getDouble("acc", 0.05);
    }

    public void execute(Graph g) {
        try {
            int steps = 100;
            System.out.println("Building figure...");
            long start = System.currentTimeMillis();
            LinkedList<Node> nodes = new LinkedList<>();
            nodes.add(new Node(0, 11));
            nodes.add(new Node(0, 6.5));
            nodes.add(new Node(-0.5, 6.5));
            nodes.add(new Node(-0.5, 5.5));
            nodes.add(new Node(0, 5.5));
            nodes.add(new Node(0, 0));
            nodes.add(new Node(11, 0));
            nodes.add(new Node(11, 11));
            long stop = System.currentTimeMillis();
            System.out.println("Figure built! Building time: " + 1.0 * (stop - start) / 1000 + "s");
            System.out.println("Calculating interfercences...");
            double [] interferences = new double[steps+1];
            MetricDistance md = new MetricDistance();
            for (int i=1; i<=steps; i++) {
                double s = 1.*i/steps;
                Path p = createPath(nodes, md);
                interferences[i] =  p.getInterference(s, md);
            }
            System.out.println("Interferences calculated! Calculation time: " + 1.0 * (stop - start) / 1000 + "s");
            System.out.println("interfs = " + arrToStr(interferences));
        } catch (Exception e) {

        }
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
    public String arrToStr(double [] arr) {
        String out = "[";
        for (int i = 0; i<arr.length; i++) {
            if (i!=0) out += ", ";
            out += arr[i];
        }
        return  out + "]";
    }
}
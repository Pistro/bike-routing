package routing.main.command;

import org.json.simple.JSONObject;
import org.xml.sax.XMLReader;
import routing.IO.JsonWriter;
import routing.IO.NodeWriter;
import routing.IO.XMLGraphWriter;
import routing.algorithms.exact.LengthReachFinder;
import routing.algorithms.exact.ReachFinder;
import routing.algorithms.heuristics.PointSelector;
import routing.graph.Graph;
import routing.graph.Node;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;
import routing.main.Main;

import javax.xml.parsers.SAXParserFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Pieter on 6/06/2016.
 */
public class SelectNodes extends Command {
    private String out;
    private int nr;
    private int attempts;

    public SelectNodes() {}

    public SelectNodes(ArgParser a) {
        super(a);
    }

    public boolean loadNodes() { return true; }

    public String getName() {
        return "selectNodes";
    }

    protected void initialize(ArgParser ap) {
        out = ap.getString("out");
        nr = ap.getInt("nr");
        attempts = ap.getInt("attempts", 20);
    }

    public void execute(Graph g) {
        PointSelector ps = new PointSelector(g, nr, attempts);
        System.out.println("Selecting points...");
        long start = System.currentTimeMillis();
        ps.execute();
        long stop = System.currentTimeMillis();
        System.out.println("Points selected! Selection time: " + (stop-start)/1000.0 + "s");
        System.out.println("Writing points...");
        start = System.currentTimeMillis();
        NodeWriter nw  = new NodeWriter();
        for (Node n: ps.selected) nw.add(n);
        JSONObject nodes = new JSONObject();
        nodes.put("nodes", nw.toJSON());
        JsonWriter jw = new JsonWriter(nodes);
        jw.write(out);
        stop = System.currentTimeMillis();
        System.out.println("Points written! Writing time: " + (stop-start)/1000.0 + "s");
    }
}

package routing.main.command;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import routing.IO.JsonWriter;
import routing.IO.XMLSPGraphReader;
import routing.algorithms.exact.*;
import routing.graph.Graph;
import routing.graph.Path;
import routing.graph.SPGraph;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;
import routing.main.Main;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;

/**
 * Created by pieter on 5/06/2016.
 */
public class FindLengthE extends Command {
    private WeightBalancer wb;
    private WeightBalancer wbReach;
    private long startId;
    private double minLength;
    private double maxLength;
    private String out;
    private double lambda;
    private double s;
    private double reach;
    private long time;
    private String hyperIn;

    public FindLengthE() {}

    public FindLengthE(ArgParser a) {
        super(a);
    }

    public String getName() {
        return "lengthE";
    }

    protected void initialize(ArgParser ap) {
        // Required arguments
        startId = ap.getLong("startId");
        minLength = ap.getDouble("minLength");
        maxLength = ap.getDouble("maxLength");
        out = ap.getString("out");
        // Choice
        hyperIn = ap.getString("hyperIn", null);
        reach = ap.getDouble("reach", -1);
        if (hyperIn==null && reach==-1) throw new IllegalArgumentException("Either reach or hyperIn should be specified!");
        // Optionals
        wb = new WeightBalancer(ap.getDouble("wFast", 0), ap.getDouble("wAttr", 0.5), ap.getDouble("wSafe", 0.5));
        wbReach = new WeightBalancer(ap.getDouble("wbFast", 0.5), ap.getDouble("wbAttr", 0.25), ap.getDouble("wbSafe", 0.25));
        s = ap.getDouble("s", 0.4);
        lambda = ap.getDouble("lambda", 12);
        time = ap.getLong("time", -1);
    }

    public void execute(Graph g) {
        try {
            long start, stop;
            SPGraph g2;
            if (hyperIn!=null) {
                System.out.println("Reading hypergraph...");
                start = System.currentTimeMillis();
                XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                XMLSPGraphReader gr = new XMLSPGraphReader(g);
                xmlReader.setContentHandler(gr);
                xmlReader.parse(Main.convertToFileURL(hyperIn));
                g2 = gr.getSpGraph();
                stop = System.currentTimeMillis();
                System.out.println("Hypergraph Read! Reading time: " + (stop-start)/1000. + "s");
                if (g2.getBi()) System.out.println("Warning: Exact routing on a bidirectional graph is slow!");
            } else {
                System.out.println("Extracting subgraph...");
                start = System.currentTimeMillis();
                g = g.getSubgraph(g.getNode(startId), maxLength);
                stop = System.currentTimeMillis();
                System.out.println("Extraction finished! Extraction time: " + (stop-start)/1000. + "s");
                System.out.println("Creating hypergraph...");
                start = System.currentTimeMillis();
                g2 = new SPGraph(g, reach, false, wbReach);
                stop = System.currentTimeMillis();
                System.out.println("Hypergraph created! Creation time: " + (stop-start)/1000. + "s");
            }
            System.out.println("Starting routing (length: " + minLength/1000. + "-" + maxLength/1000. + "km)...");
            ExhaustiveRouteLengthFinder rlf = new ExhaustiveRouteLengthFinder(g.getNode(startId), wb, lambda, s, minLength, maxLength, g2);
            rlf.maxSearchTimeMs = time;
            start = System.currentTimeMillis();
            Path p = rlf.findRoute();
            stop = System.currentTimeMillis();
            System.out.println("Routing finished! Routing time: " + (stop-start)/1000. + "s");
            if (p != null) {
                System.out.println("Writing routes to '" + out + "'...");
                JSONObject j = new JSONObject();
                double weight = p.getWeight(wb)/p.getLength();
                double interference = lambda*p.getInterference(s);
                p.addTag("weight", weight);
                p.addTag("interf", interference);
                p.addTag("score", weight + interference);
                JSONArray routes = new JSONArray();
                routes.add(p.toJSON());
                j.put("routes", routes);
                new JsonWriter(j).write(out);
                System.out.println("Routes written!");
            } else {
                if (rlf.getScore() == Double.MAX_VALUE) System.out.println("No route found!");
                else System.out.println("Score under bound: " + rlf.getScore());
            }
        } catch (ParserConfigurationException|IOException|SAXException e) {
            e.printStackTrace();
        }
    }
}

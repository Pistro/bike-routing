package routing.main.command;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import routing.IO.JsonWriter;
import routing.IO.XMLSPGraphReader;
import routing.algorithms.candidateselection.CandidateSelector;
import routing.algorithms.candidateselection.DistPlSelector;
import routing.algorithms.heuristics.RouteLengthFinder;
import routing.graph.*;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;
import routing.main.Main;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by pieter on 5/06/2016.
 */
public class FindLength extends Command {
    private WeightBalancer wb;
    private WeightBalancer wbReach;
    private long startId;
    private double minLength;
    private double maxLength;
    private int alternatives;
    private String out;
    private double lambda;
    private double strictness;
    private String hyperIn;
    private double reach;

    public FindLength() {}

    public FindLength(ArgParser a) {
        super(a);
    }

    public String getName() {
        return "length";
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
        alternatives = (int) ap.getLong("alt", 8);
        wb = new WeightBalancer(ap.getDouble("wFast", 0), ap.getDouble("wAttr", 0.5), ap.getDouble("wSafe", 0.5));
        wbReach = new WeightBalancer(ap.getDouble("wbFast", 0.5), ap.getDouble("wbAttr", 0.25), ap.getDouble("wbSafe", 0.25));
        lambda = ap.getDouble("lambda", 12);
        strictness = ap.getDouble("strictness", 0.4);
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
            } else {
                System.out.println("Extracting subgraph...");
                start = System.currentTimeMillis();
                g = g.getSubgraph(g.getNode(startId), maxLength);
                stop = System.currentTimeMillis();
                System.out.println("Extraction finished! Extraction time: " + (stop-start)/1000. + "s");
                System.out.println("Creating hypergraph...");
                start = System.currentTimeMillis();
                g2 = new SPGraph(g, reach, true, wbReach);
                stop = System.currentTimeMillis();
                System.out.println("Hypergraph created! Creation time: " + (stop-start)/1000. + "s");
            }
            System.out.println("Starting routing (length: " + minLength/1000. + "-" + maxLength/1000. + "km, " + alternatives + " attempts)...");
            CandidateSelector cs = new DistPlSelector(g.getNode(startId));
            RouteLengthFinder rlf = new RouteLengthFinder(wb, g.getNode(startId), cs, minLength, maxLength, lambda, strictness, alternatives, g2);
            start = System.currentTimeMillis();
            LinkedList<Path> paths = rlf.findRoutes();
            stop = System.currentTimeMillis();
            System.out.println("Starting routing (length: " + minLength/1000. + "-" + maxLength/1000. + "km, " + alternatives + " attempts)! Routing time: " + (stop-start)/1000. + "s");
            System.out.println("Extraction time: " + rlf.extractionTime / 1000. + "s");
            System.out.println("Forward time: " + rlf.forwardTime / 1000. + "s");
            System.out.println("Backward time (avg): " + rlf.backwardTime / (1000. * alternatives) + "s");
            System.out.println("Writing routes to '" + out + "'...");
            JSONArray routes = new JSONArray();
            for (Path p : paths) {
                double weight = p.getWeight(wb)/p.getLength();
                double interference = lambda*p.getInterference(strictness);
                p.addTag("weight", weight);
                p.addTag("interf", interference);
                p.addTag("score", weight+interference);
                routes.add(p.toJSON());
            }
            JSONObject j = new JSONObject();
            j.put("routes", routes);
            new JsonWriter(j).write(out);
            System.out.println("Routes written!");
        } catch (SAXException|ParserConfigurationException|IOException e) {
            e.printStackTrace();
        }
    }
}

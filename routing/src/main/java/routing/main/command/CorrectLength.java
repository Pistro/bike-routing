package routing.main.command;

import org.xml.sax.XMLReader;
import routing.IO.XMLGraphWriter;
import routing.graph.Edge;
import routing.graph.Graph;
import routing.graph.Node;
import routing.main.ArgParser;
import routing.main.Main;

import javax.xml.parsers.SAXParserFactory;
import java.util.HashMap;

/**
 * Adapted edge lengths to make sure they are larger than the distance between the start and end of an edge
 * Created by Pieter on 6/06/2016.
 */
public class CorrectLength extends Command {
    private String in;
    private String out;
    public CorrectLength() {}

    public CorrectLength(ArgParser a) {
        super(a);
    }

    public String getName() {
        return "correctLength";
    }

    protected void initialize(ArgParser ap) {
        out = ap.getString("out");
        in = Main.convertToFileURL(ap.getString("in"));
    }

    public void execute(Graph g) {
        HashMap<Integer, HashMap<String, String>> newLengths = new HashMap<>();
        System.out.println("Correcting edge lengths...");
        long start = System.currentTimeMillis();
        for (Node n: g.getNodes().values()) {
            for (Edge e: n.getOutEdges()) {
                double minLength = Math.ceil(100*e.getStart().getDistance(e.getStop()))/100.;
                if (e.getLength()<minLength) {
                    HashMap<String, String> edgeAttrs = new HashMap<>();
                    edgeAttrs.put("length", Double.toString(minLength));
                    newLengths.put(e.getId(), edgeAttrs);
                }
            }
        }
        long stop = System.currentTimeMillis();
        System.out.println("Correction finished! Correction time: " + 1.0*(stop-start)/1000 + "s");
        System.out.println("Writing corrected graph...");
        try {
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            XMLGraphWriter xmlGw = new XMLGraphWriter(out);
            xmlGw.setWayTagUpdates(newLengths);
            xmlReader.setContentHandler(xmlGw);
            start = System.currentTimeMillis();
            xmlReader.parse(in);
            stop = System.currentTimeMillis();
        } catch (Exception e) {
            // XML parser exceptions and stuff. Should never occur...
        }
        System.out.println("Corrected graph written. Writing time: " + 1.0*(stop-start)/1000);

    }
}


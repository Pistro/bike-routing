package routing.main.command;

import org.xml.sax.XMLReader;
import routing.IO.XMLGraphWriter;
import routing.algorithms.exact.ComponentDiscovery;
import routing.algorithms.exact.SimpleContractor;
import routing.graph.Graph;
import routing.graph.Node;
import routing.main.ArgParser;
import routing.main.Main;

import javax.xml.parsers.SAXParserFactory;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by Pieter on 6/06/2016.
 */
public class Contract extends Command {
    private String in;
    private String out;
    public Contract() {}

    public Contract(ArgParser a) {
        super(a);
    }

    public boolean loadNodes() { return false; }

    public boolean loadFullEdges() { return true; }

    public String getName() {
        return "contract";
    }

    protected void initialize(ArgParser ap) {
        out = ap.getString("out");
        in = Main.convertToFileURL(ap.getString("in"));
    }

    public void execute(Graph g) {
        System.out.println("Contracting the graph...");
        int oldSize = g.getNodes().size();
        long start = System.currentTimeMillis();
        SimpleContractor c = new SimpleContractor(g);
        long stop = System.currentTimeMillis();
        System.out.println("Contraction finished! Contraction time: " + 1.0*(stop-start)/1000 + "s");
        System.out.println("Original graph order: " + oldSize + ", contr. graph order: " + g.getNodes().size() + ", fraction: " + ((float) g.getNodes().size()/oldSize));
        System.out.println("Writing contracted graph...");
        try {
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            XMLGraphWriter xmlGw = new XMLGraphWriter(out);
            xmlGw.setNodeWhiteList(g.getNodes().keySet());
            xmlGw.setNewWays(c.getNewEdges());
            xmlReader.setContentHandler(xmlGw);
            start = System.currentTimeMillis();
            xmlReader.parse(in);
            stop = System.currentTimeMillis();
            System.out.println("Contracted graph written. Writing time: " + 1.0*(stop-start)/1000);
        } catch (Exception e) {
            // XML parser exceptions and stuff. Should never occur...
        }
    }
}

package routing.main.command;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import routing.IO.XMLGraphWriter;
import routing.algorithms.candidateselection.DistPlSelector;
import routing.algorithms.exact.ComponentDiscovery;
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
public class FindLCC extends Command {
    private String in;
    private String out;
    public FindLCC() {}

    public FindLCC(ArgParser a) {
        super(a);
    }

    public String getName() {
        return "lcc";
    }

    protected void initialize(ArgParser ap) {
        out = ap.getString("out");
        in = Main.convertToFileURL(ap.getString("in"));
    }

    public boolean loadNodes() { return false; }

    public void execute(Graph g) {
        System.out.println("Starting component detection (Tarjan)...");
        long start = System.currentTimeMillis();
        LinkedList<Set<Node>> components = new ComponentDiscovery(g).getComponents();
        long stop = System.currentTimeMillis();
        System.out.println("Component detection finished! Detection time: " + 1.0 * (stop - start) / 1000 + "s");
        System.out.println("Extracting largest connected component...");
        start = System.currentTimeMillis();
        int maxSize = 0;
        Set<Node> largestConnectedComponent = new HashSet<Node>();
        for (Set<Node> comp : components) {
            if (comp.size() > maxSize) {
                maxSize = comp.size();
                largestConnectedComponent = comp;
            }
        }
        components.clear();
        Set<Long> largestConnectedComponentIds = new HashSet<Long>();
        for (Node n : largestConnectedComponent) largestConnectedComponentIds.add(n.getId());
        largestConnectedComponent.clear();
        stop = System.currentTimeMillis();
        System.out.println("Largest connected component finished. Extraction time: " + 1.0 * (stop - start) / 1000 + "s");
        System.out.println("Original graph order: " + g.getNodes().size() + ", largest conn. comp. order: " + largestConnectedComponentIds.size() + ", fraction: " + ((float) largestConnectedComponentIds.size() / g.getNodes().size()));
        System.out.println("Writing largest connected component to '" + out + "'...");
        try {
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            XMLGraphWriter xmlGw = new XMLGraphWriter(out);
            xmlGw.setNodeWhiteList(largestConnectedComponentIds);
            xmlReader.setContentHandler(xmlGw);
            start = System.currentTimeMillis();
            xmlReader.parse(in);
            stop = System.currentTimeMillis();
        } catch (Exception e) {
            // XML parser exceptions and stuff. Should never occur...
        }
        System.out.println("Largest connected component written. Write time: " + 1.0 * (stop - start) / 1000 + "s");
    }
}

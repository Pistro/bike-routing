package routing.main.command;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import routing.IO.XMLSPGraphReader;
import routing.graph.Graph;
import routing.graph.SPGraph;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;
import routing.main.Main;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;

/**
 * Created by piete on 27/12/2016.
 */
public class CollectInfo extends Command {
    private WeightBalancer wbReach;
    private double reach;
    private String hyperIn;

    public CollectInfo() {}

    public CollectInfo(ArgParser a) {
        super(a);
    }

    public String getName() {
        return "info";
    }

    protected void initialize(ArgParser ap) {
        // Optional arguments
        hyperIn = ap.getString("hyperIn", null);
        reach = ap.getDouble("reach", -1);
        wbReach = new WeightBalancer(ap.getDouble("wbFast", 0.5), ap.getDouble("wbAttr", 0.25), ap.getDouble("wbSafe", 0.25));
    }

    public void execute(Graph g) {
        try {
            long start, stop;
            if (reach != -1) {
                System.out.println("Reading hypergraph...");
                start = System.currentTimeMillis();
                XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                XMLSPGraphReader gr = new XMLSPGraphReader(g);
                xmlReader.setContentHandler(gr);
                xmlReader.parse(Main.convertToFileURL(hyperIn));
                g = gr.getSpGraph();
                stop = System.currentTimeMillis();
                System.out.println("Hypergraph Read! Reading time: " + (stop - start) / 1000. + "s");
            } else if (hyperIn!=null) {
                System.out.println("Creating hypergraph...");
                start = System.currentTimeMillis();
                g = new SPGraph(g, reach, false, wbReach);
                stop = System.currentTimeMillis();
                System.out.println("Hypergraph created! Creation time: " + (stop - start) / 1000. + "s");
            }
            System.out.println("# vertices: " + g.getOrder());
            System.out.println("# edges: " + g.getSize());
        } catch (ParserConfigurationException |IOException |SAXException e) {
            e.printStackTrace();
        }
    }
}

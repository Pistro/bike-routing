package routing.IO;

import routing.graph.Edge;
import routing.graph.Node;
import routing.graph.SPGraph;

import java.io.IOException;
import java.util.*;

/**
 * Created by pieter on 27/02/2016.
 */
public class XMLSPGraphWriter {
    private final XMLWriter xmlWr;
    private final SPGraph g;

    public XMLSPGraphWriter(SPGraph g, String outPath) throws IOException {
        this.g = g;
        xmlWr = new XMLWriter(outPath);
    };

    public void write() {
        xmlWr.startDocument();
        HashMap<String, String> graphAttr = new HashMap<>();
        graphAttr.put("version", "0.6");
        graphAttr.put("generator", "Osmosis 0.44.1");
        graphAttr.put("reach", Double.toString(g.getReach()));
        graphAttr.put("bi", Integer.toString(g.getBi()? 1 : 0));
        graphAttr.put("wAttr", Double.toString(g.getWeightBalancer().getWAttr()));
        graphAttr.put("wFast", Double.toString(g.getWeightBalancer().getWFast()));
        graphAttr.put("wSafe", Double.toString(g.getWeightBalancer().getWSafe()));
        xmlWr.startElement("osm", graphAttr);
        for (Node n: g.getNodes().values()) {
            SPGraph.NodePair np = (SPGraph.NodePair) n;
            HashMap<String, String> nodeAttr = new HashMap<>();
            nodeAttr.put("id", Long.toString(np.getId()));
            nodeAttr.put("start_id", Long.toString(np.s.getId()));
            nodeAttr.put("stop_id", Long.toString(np.e.getId()));
            HashMap<String, String> nodeTags = new HashMap<>();
            if (np.hasReach()) nodeTags.put("reach", Double.toString(n.getReach()));
            xmlWr.startElement("node", nodeAttr);
            for (Map.Entry<String, String> en: nodeTags.entrySet()) {
                HashMap<String, String> tagAttrs = new HashMap<>();
                tagAttrs.put("k", en.getKey());
                tagAttrs.put("v", en.getValue());
                xmlWr.startElement("tag", tagAttrs);
                xmlWr.endElement("tag");
            }
            xmlWr.endElement("node");
        }
        for (Node n: g.getNodes().values()) {
            for (Edge e: n.getOutEdges()) {
                HashMap<String, String> wayAttr = new HashMap<>();
                wayAttr.put("id", Integer.toString(e.id));
                HashMap<String, String> wayTags = new HashMap<>();
                wayTags.put("start_node", Long.toString(e.getStart().getId()));
                wayTags.put("end_node", Long.toString(e.getStop().getId()));
                xmlWr.startElement("way", wayAttr);
                for (Map.Entry<String, String> en: wayTags.entrySet()) {
                    HashMap<String, String> tagAttrs = new HashMap<>();
                    tagAttrs.put("k", en.getKey());
                    tagAttrs.put("v", en.getValue());
                    xmlWr.startElement("tag", tagAttrs);
                    xmlWr.endElement("tag");
                }
                xmlWr.endElement("way");
            }
        }
        xmlWr.endElement("osm");
        xmlWr.endDocument();
    }
}

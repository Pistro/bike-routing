package routing.IO;

import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.util.Pair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import routing.graph.Edge;
import routing.graph.FullEdge;
import routing.graph.Graph;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;

/**
 * Created by Pieter on 27/02/2016.
 */
public class XMLGraphWriter extends DefaultHandler {
    private Set<Long> nodeWhiteList = null;
    private LinkedList<FullEdge> newWays = new LinkedList<>();
    private HashMap<Long, HashMap<String, String>> nodeAttrUpdates = new HashMap<Long, HashMap<String, String>>();
    private HashMap<Long, HashMap<String, String>> nodeTagUpdates = new HashMap<Long, HashMap<String, String>>();
    private HashMap<String, String> curAttrs = new HashMap<String, String>();
    private HashMap<String, String> curTags = new HashMap<String, String>();
    private ArrayList<Integer> curWays = new ArrayList<Integer>();
    private boolean inSmallElement = false;

    private XMLWriter xmlWr;
    public XMLGraphWriter(String outPath) throws IOException {
        xmlWr = new XMLWriter(outPath);
    };
    public void setNodeWhiteList(Set<Long> whiteList) {
        this.nodeWhiteList = whiteList;
    }
    public void setNodeAttrUpdates(HashMap<Long, HashMap<String, String>> updates) { this.nodeAttrUpdates = updates; }
    public void setNodeTagUpdates(HashMap<Long, HashMap<String, String>> updates) {
        this.nodeTagUpdates = updates;
    }
    public void setNewWays(LinkedList<FullEdge> newWays) {
        this.newWays = newWays;
    }

    @Override
    public void startDocument() throws SAXException {
        xmlWr.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        xmlWr.endDocument();
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes attrs) throws SAXException {
        if (qName.equals("node")) {
            inSmallElement = true;
            curAttrs.clear();
            curTags.clear();
            curWays.clear();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i<nrAttrs; i++) curAttrs.put(attrs.getQName(i), attrs.getValue(i));
        } else if (qName.equals("way")) {
            inSmallElement = true;
            curAttrs.clear();
            curTags.clear();
            curWays.clear();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i<nrAttrs; i++) curAttrs.put(attrs.getQName(i), attrs.getValue(i));
        } else if (inSmallElement) {
            if (qName.equals("tag")) {
                curTags.put(attrs.getValue("k"), attrs.getValue("v"));
            } else if (qName.equals("wy")) {
                curWays.add(Integer.parseInt(attrs.getValue("ref")));
            }
        } else {
            xmlWr.startElement(qName, attrs);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        boolean keep = true;
        if (qName.equals("node")) {
            long nodeId = Long.parseLong(curAttrs.get("id"));
            if (nodeAttrUpdates.containsKey(nodeId)) {
                curAttrs.putAll(nodeAttrUpdates.get(nodeId));
            }
            if (nodeTagUpdates.containsKey(nodeId)) {
                curTags.putAll(nodeTagUpdates.get(nodeId));
            }
            keep = (nodeWhiteList==null) || nodeWhiteList.contains(nodeId);
        } else if (qName.equals("way")) {
            keep = (nodeWhiteList==null)
                    || (nodeWhiteList.contains(Long.parseLong(curTags.get("start_node"))) && nodeWhiteList.contains(Long.parseLong(curTags.get("end_node"))));
        }
        if (qName.equals("way") || qName.equals("node")) {
            inSmallElement = false;
            if (keep) {
                xmlWr.startElement(qName, curAttrs);
                HashMap<String, String> temp = new HashMap<String, String>();
                for (Integer wayRef : curWays) {
                    temp.put("ref", Integer.toString(wayRef));
                    xmlWr.startElement("wy", temp);
                    xmlWr.endElement("wy");
                }
                temp.clear();
                for (Map.Entry<String, String> en: curTags.entrySet()) {
                    temp.put("k", en.getKey());
                    temp.put("v", en.getValue());
                    xmlWr.startElement("tag", temp);
                    xmlWr.endElement("tag");
                }
                xmlWr.endElement(qName);
            }
        } else if (qName.equals("osm")) {
            HashMap<Integer, Pair<FullEdge, Boolean>> idToEdgeOneWay = new HashMap<>();
            for (FullEdge e: newWays) {
                Pair<FullEdge, Boolean> present = idToEdgeOneWay.get(e.getId());
                if (present==null) idToEdgeOneWay.put(e.getId(), new Pair<>(e, false));
                else if (present.getKey().getStart()==e.getStop() && present.getKey().getStop()==e.getStart()) idToEdgeOneWay.put(e.getId(), new Pair<>(e, true));
            }
            for (Pair<FullEdge, Boolean> p : idToEdgeOneWay.values()) {
                FullEdge e = p.getKey();
                curAttrs.clear();
                curTags.clear();
                curAttrs.put("id", Integer.toString(e.getId()));
                curAttrs.putAll(e.getAttrs());
                curTags.putAll(e.getTags());
                curTags.put("bicycle_oneway", p.getValue()? "0" : "1");
                xmlWr.startElement("way", curAttrs);
                HashMap<String, String> tmp = new HashMap<String, String>();
                for (int i = 0; i<e.shadow.length; i++) {
                    tmp.put("ref", Integer.toString(e.shadow[i]));
                    xmlWr.startElement("wy", tmp);
                    xmlWr.endElement("wy");
                }
                tmp.clear();
                for (Map.Entry<String, String> en: curTags.entrySet()) {
                    tmp.put("k", en.getKey());
                    tmp.put("v", en.getValue());
                    xmlWr.startElement("tag", tmp);
                    xmlWr.endElement("tag");
                }
                xmlWr.endElement("way");
            }
            xmlWr.endElement(qName);
        } else if (!inSmallElement) {
            xmlWr.endElement(qName);
        }
    }

}

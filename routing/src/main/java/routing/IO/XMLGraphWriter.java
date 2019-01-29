package routing.IO;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import routing.graph.Edge;
import routing.graph.Node;

import java.io.IOException;
import java.util.*;

/**
 * Created by Pieter on 27/02/2016.
 */
public class XMLGraphWriter extends DefaultHandler {
    private Set<Long> nodeWhiteList = null;
    private Set<Integer> wayWhiteList = null;
    private LinkedList<Edge> newWays = new LinkedList<>();
    private HashMap<Long, HashMap<String, String>> nodeAttrUpdates = new HashMap<>();
    private HashMap<Long, HashMap<String, String>> nodeTagUpdates = new HashMap<>();
    private HashMap<Integer, HashMap<String, String>> wayAttrUpdates = new HashMap<>();
    private HashMap<Integer, HashMap<String, String>> wayTagUpdates = new HashMap<>();
    private HashMap<String, String> curAttrs = new HashMap<>();
    private HashMap<String, String> curTags = new HashMap<>();
    private LinkedList<Long> curNodeIds = new LinkedList<>();
    private boolean inSmallElement = false;

    private XMLWriter xmlWr;
    public XMLGraphWriter(String outPath) throws IOException {
        xmlWr = new XMLWriter(outPath);
    };
    public void setNodeWhiteList(Set<Long> whiteList) {
        this.nodeWhiteList = whiteList;
    }
    public void setWayWhiteList(Set<Integer> whiteList) {
        this.wayWhiteList = whiteList;
    }
    public void setNodeAttrUpdates(HashMap<Long, HashMap<String, String>> updates) { this.nodeAttrUpdates = updates; }
    public void setNodeTagUpdates(HashMap<Long, HashMap<String, String>> updates) {
        this.nodeTagUpdates = updates;
    }
    public void setWayAttrUpdates(HashMap<Integer, HashMap<String, String>> updates) { this.wayAttrUpdates = updates; }
    public void setWayTagUpdates(HashMap<Integer, HashMap<String, String>> updates) {
        this.wayTagUpdates = updates;
    }
    public void setNewWays(LinkedList<Edge> newWays) {
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
            curNodeIds.clear();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i<nrAttrs; i++) curAttrs.put(attrs.getQName(i), attrs.getValue(i));
        } else if (qName.equals("way")) {
            inSmallElement = true;
            curAttrs.clear();
            curTags.clear();
            curNodeIds.clear();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i<nrAttrs; i++) curAttrs.put(attrs.getQName(i), attrs.getValue(i));
        } else if (inSmallElement) {
            if (qName.equals("tag")) {
                curTags.put(attrs.getValue("k"), attrs.getValue("v"));
            } else if (qName.equals("nd")) {
                curNodeIds.add(Long.parseLong(attrs.getValue("ref")));
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
            int wayId = Integer.parseInt(curAttrs.get("id"));
            if (wayAttrUpdates.containsKey(wayId)) {
                curAttrs.putAll(wayAttrUpdates.get(wayId));
            }
            if (wayTagUpdates.containsKey(wayId)) {
                curTags.putAll(wayTagUpdates.get(wayId));
            }
            keep = (nodeWhiteList==null)
                    || (nodeWhiteList.contains(curNodeIds.getFirst()) && nodeWhiteList.contains(curNodeIds.getLast()));
            keep &= (wayWhiteList == null) || wayWhiteList.contains(wayId);
        }
        if (qName.equals("way") || qName.equals("node")) {
            inSmallElement = false;
            if (keep) {
                xmlWr.startElement(qName, curAttrs);
                HashMap<String, String> temp = new HashMap<String, String>();
                for (Long nodeId : curNodeIds) {
                    temp.put("ref", Long.toString(nodeId));
                    xmlWr.startElement("nd", temp);
                    xmlWr.endElement("nd");
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
            for (Edge e: newWays) {
                curAttrs.clear();
                curTags.clear();
                curAttrs.put("id", Integer.toString(e.getId()));
                curTags.put("bicycle_oneway", "1");
                curTags.put("score_safe", Double.toString(e.getWSafe()));
                curTags.put("score_attr", Double.toString(e.getWAttr()));
                curTags.put("score_fast", Double.toString(e.getWFast()));
                curTags.put("length", Double.toString(e.getLength()));
                if (wayAttrUpdates.containsKey(e.getId())) {
                    curAttrs.putAll(wayAttrUpdates.get(e.getId()));
                }
                if (wayTagUpdates.containsKey(e.getId())) {
                    curTags.putAll(wayTagUpdates.get(e.getId()));
                }
                xmlWr.startElement("way", curAttrs);
                HashMap<String, String> tmp = new HashMap<>();
                tmp.put("ref", Long.toString(e.getStart().getId()));
                xmlWr.startElement("nd", tmp);
                xmlWr.endElement("nd");
                for (Node n: e.intermediateNodes) {
                    tmp.put("ref", Long.toString(n.getId()));
                    xmlWr.startElement("nd", tmp);
                    xmlWr.endElement("nd");
                }
                tmp.put("ref", Long.toString(e.getStop().getId()));
                xmlWr.startElement("nd", tmp);
                xmlWr.endElement("nd");
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

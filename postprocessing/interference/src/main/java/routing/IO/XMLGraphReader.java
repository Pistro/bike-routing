/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.IO;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import routing.graph.*;

/**
 *
 * @author Pieter
 */
public class XMLGraphReader extends DefaultHandler {
    private final Graph graph = new Graph();
    private boolean keep = false;
    private final HashMap<String, String> curAttrs = new HashMap<>();
    private final HashMap<String, String> curTags = new HashMap<>();
    private final LinkedList<Long> nodes = new LinkedList<>();
    private boolean dynamicNodes = false;

    public Graph getGraph() { return graph; }
    public void setDynamicNodes(boolean b) { this.dynamicNodes = b; }
    
    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes attrs) throws SAXException {
        if (qName.equals("node") && !dynamicNodes) {
            curAttrs.clear();
            curTags.clear();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i<nrAttrs; i++) curAttrs.put(attrs.getQName(i), attrs.getValue(i));
            keep = true;
        } else if (qName.equals("way")) {
            curAttrs.clear();
            curTags.clear();
            nodes.clear();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i<nrAttrs; i++) curAttrs.put(attrs.getQName(i), attrs.getValue(i));
            keep = true;
        } else if (keep) {
            if (qName.equals("tag")) curTags.put(attrs.getValue("k"), attrs.getValue("v"));
            else if (qName.equals("nd")) nodes.add(Long.parseLong(attrs.getValue("ref")));
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) {
        if (keep) {
            if (qName.equals("node") && !dynamicNodes) {
                keep = false;
                Node n = new Node(Long.parseLong(curAttrs.get("id")), Double.parseDouble(curAttrs.get("lat")), Double.parseDouble(curAttrs.get("lon")));
                graph.addNode(n);
            } else if (qName.equals("way")) {
                keep = false;
                int id = Integer.parseInt(curAttrs.get("id"));
                double length = Double.parseDouble(curTags.get("length"));
                Node start, stop;
                if (!dynamicNodes) {
                    start = graph.getNode(nodes.get(0));
                    stop = graph.getNode(nodes.get(nodes.size()-1));
                } else {
                    start = graph.addNode(nodes.get(0), 0, 0);
                    stop = graph.addNode(nodes.get(nodes.size()-1), 0, 0);
                }
                Edge e1 = new Edge(id, start, stop, length);
                e1.couple();
                int nrNodes = nodes.size();
                e1.shadow = new Long[nrNodes];
                for (int i = 0; i<nrNodes; i++) e1.shadow[i] = nodes.get(i);
            }
        }
    }
}

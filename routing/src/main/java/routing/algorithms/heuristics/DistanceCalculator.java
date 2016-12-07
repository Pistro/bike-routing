package routing.algorithms.heuristics;

import routing.graph.Edge;
import routing.graph.Node;
import routing.graph.SimpleNode;

/**
 * Created by piete on 29/09/2016.
 */
public class DistanceCalculator {
    private final double lonScale;
    private final double latScale;

    public DistanceCalculator(Node center) {
        lonScale = center.getDistance(new SimpleNode(0L, center.getLat(), center.getLon() + 1));
        latScale = center.getDistance(new SimpleNode(0L, center.getLat() + 1, center.getLon()));
    }
    public double getDistance(Edge e0, Edge e1) {
        double lat0 = (e0.getStart().getLat()+e0.getStop().getLat())/2;
        double lon0 = (e0.getStart().getLon()+e0.getStop().getLon())/2;
        double lat1 = (e1.getStart().getLat()+e1.getStop().getLat())/2;
        double lon1 = (e1.getStart().getLon()+e1.getStop().getLon())/2;
        return getDistance(lat0, lon0, lat1, lon1);
    }
    public double getDistance(Node n0, Node n1) {
        return getDistance(n0.getLat(), n0.getLon(), n1.getLat(), n1.getLon());
    }
    public double getDistance(Node n0, Edge e1) {
        double lat1 = (e1.getStart().getLat()+e1.getStop().getLat())/2;
        double lon1 = (e1.getStart().getLon()+e1.getStop().getLon())/2;
        return getDistance(n0.getLat(), n0.getLon(), lat1, lon1);
    }
    public double getDistance(Edge e0, Node n1) {
        double lat0 = (e0.getStart().getLat()+e0.getStop().getLat())/2;
        double lon0 = (e0.getStart().getLon()+e0.getStop().getLon())/2;
        return getDistance(lat0, lon0, n1.getLat(), n1.getLon());
    }
    public double getDistance(double lat0, double lon0, double lat1, double lon1) {
        double latDif = latScale*(lat0-lat1);
        double lonDif = lonScale*(lon0-lon1);
        return Math.round(Math.sqrt(latDif*latDif+lonDif*lonDif)*100)/100;
    }
    public double getLonScale() { return lonScale; }
    public double getLatScale() { return latScale; }
}

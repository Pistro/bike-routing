package routing.graph;

/**
 * Created by piete on 30/10/2016.
 */
public class SimpleNode extends Node {
    private final double lat;
    private final double lon;
    public SimpleNode() {
        super();
        lat = -1;
        lon = -1;
    }
    public SimpleNode(Long id, double lat, double lon) {
        super(id);
        this.lat = lat;
        this.lon = lon;
    }
    public SimpleNode(double lat, double lon) {
        super();
        this.lat = lat;
        this.lon = lon;
    }
    public SimpleNode(Long id, double lat, double lon, double reach) {
        super(id);
        this.lat = lat;
        this.lon = lon;
        setReach(reach);
    }
    public SimpleNode(SimpleNode n) {
        super(n.getId());
        this.lat = n.lat;
        this.lon = n.lon;
        setReach(n.getReach());
    }

    public double getLat() {return lat; }
    public double getLon() { return lon; }

}

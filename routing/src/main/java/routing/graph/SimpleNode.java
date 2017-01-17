package routing.graph;

/**
 * Created by piete on 30/10/2016.
 */
public class SimpleNode extends Node {
    private final double lat;
    private final double lon;
    public SimpleNode(Long id) {
        super(id);
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
    public SimpleNode clone() {
        SimpleNode out = new SimpleNode(getId(), lat, lon);
        out.setReach(getReach());
        return out;
    }

    public double getLat() {return lat; }
    public double getLon() { return lon; }

}

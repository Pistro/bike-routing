package routing.graph;

/**
 * Created by piete on 14/01/2017.
 */
public class SimpleEdge extends Edge {
    private final double wFast;
    private final double wAttr;
    private final double wSafe;

    public SimpleEdge(int id, Node start, Node stop, double length, double heightDif, double wFast, double wAttr, double wSafe) {
        super(id, start, stop, length, heightDif);
        this.wFast = wFast;
        this.wAttr = wAttr;
        this.wSafe = wSafe;
        couple();
    }
    public SimpleEdge(Edge e, Node start, Node stop) {
        this(e.getId(), start, stop, e.getLength(), e.getHeightDif(), e.getWFast(), e.getWAttr(), e.getWSafe());
        shadow = e.shadow;
    }
    private SimpleEdge(int id, double length, double heightDif, double wFast, double wAttr, double wSafe, Node start, Node stop) {
        super(id, start, stop, length, heightDif);
        this.wFast = wFast;
        this.wAttr = wAttr;
        this.wSafe = wSafe;
    }
    public static Edge getUncoupledEdge(Edge e, Node start, Node stop) {
        Edge out = new SimpleEdge(e.getId(), e.getLength(), e.getHeightDif(), e.getWFast(), e.getWAttr(), e.getWSafe(), start, stop);
        out.shadow = e.shadow;
        return out;
    }
    @Override
    public double getWFast() { return wFast; }

    @Override
    public double getWAttr() { return wAttr; }

    @Override
    public double getWSafe() { return wSafe; }
}

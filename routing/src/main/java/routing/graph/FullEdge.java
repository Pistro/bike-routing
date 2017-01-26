package routing.graph;

import java.util.HashMap;

/**
 * Created by Pieter on 14/01/2017.
 */
public class FullEdge extends Edge {
    public final double wFastLin;
    public final double wFastConst;
    public final double wAttrLin;
    public final double wAttrConst;
    public final double wSafeLin;
    public final double wSafeConst;
    public FullEdge(int id, Node start, Node stop, double length, double heightDif, double wFastConst, double wFastLin, double wAttrConst, double wAttrLin, double wSafeConst, double wSafeLin) {
        super(id, start, stop, length, heightDif);
        this.wFastConst = wFastConst;
        this.wFastLin = wFastLin;
        this.wAttrConst = wAttrConst;
        this.wAttrLin = wAttrLin;
        this.wSafeConst = wSafeConst;
        this.wSafeLin = wSafeLin;
        couple();
    }

    @Override
    public double getWFast() { return wFastLin*getLength()+wFastConst; }

    @Override
    public double getWAttr() { return wAttrLin*getLength()+wAttrConst; }

    @Override
    public double getWSafe() { return wSafeLin*getLength()+wSafeConst; }

    public static FullEdge join(FullEdge start, FullEdge stop) {
        return join(Edge.getFreeId(), start, stop);
    }

    public static FullEdge join(int id, FullEdge start, FullEdge stop) {
        if (start.getStop() != stop.getStart()) throw new IllegalArgumentException("Attached edge should start where the attaching edge ends");
        double outLen = Math.round((start.getLength()+stop.getLength())*100)/100.;
        FullEdge out = new FullEdge(id, start.getStart(), stop.getStop(), outLen,Math.round((start.getHeightDif()+stop.getHeightDif())*100)/100.,
                start.wFastConst+stop.wFastConst, outLen!=0? (start.wFastLin*start.getLength()+stop.wFastLin*stop.getLength())/outLen : 0,
                start.wAttrConst+stop.wAttrConst, outLen!=0? (start.wAttrLin*start.getLength()+stop.wAttrLin*stop.getLength())/outLen : 0,
                start.wSafeConst+stop.wSafeConst, outLen!=0? (start.wSafeLin*start.getLength()+stop.wSafeLin*stop.getLength())/outLen : 0);
        out.shadow = new int[start.shadow.length+stop.shadow.length];
        System.arraycopy(start.shadow, 0, out.shadow, 0, start.shadow.length);
        System.arraycopy(stop.shadow, 0, out.shadow, start.shadow.length, stop.shadow.length);
        return out;
    }

    public HashMap<String, String> getTags() {
        HashMap<String, String> out = new HashMap<String, String>();
        out.put("start_node", Long.toString(getStart().getId()));
        out.put("end_node", Long.toString(getStop().getId()));
        out.put("height_dif", Double.toString(getHeightDif()));
        out.put("length", Double.toString(getLength()));
        out.put("score_safe_lin", Double.toString(wSafeLin));
        out.put("score_safe_const", Double.toString(wSafeConst));
        out.put("score_attr_lin", Double.toString(wAttrLin));
        out.put("score_attr_const", Double.toString(wAttrConst));
        out.put("score_fast_lin", Double.toString(wFastLin));
        out.put("score_fast_const", Double.toString(wFastConst));
        return out;
    }

    //public boolean isAntiparallel(FullEdge e) {
    //    if (getStart()!=e.getStop() || getStop()!=e.getStart())
    //}

}

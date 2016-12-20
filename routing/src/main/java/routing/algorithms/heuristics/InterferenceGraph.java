package routing.algorithms.heuristics;

import routing.graph.Edge;
import routing.graph.Node;
import routing.graph.Path;
import routing.graph.SimpleNode;

import java.util.LinkedList;

/**
 * Created by piete on 10/12/2016.
 */
public class InterferenceGraph {
    private final DistanceCalculator dc;
    private final InterferenceNode lowerStart;
    private final double pLen;
    private final double strictness;

    public InterferenceGraph(Path p, double s, double t) {
        strictness = s;
        dc = new DistanceCalculator(p.getStart());
        LinkedList<InterferenceNode> prevLevel = new LinkedList<>();
        InterferenceNode prevNode = null;
        double pos = 0;
        for (Edge e: p.getEdges()) {
            InterferenceNode curNode = new InterferenceNode(new ApproximateEdge(e, pos));
            prevLevel.add(curNode);
            if (prevNode!=null) curNode.setLeft(prevNode);
            pos += e.getLength();
            prevNode = curNode;
        }
        lowerStart = prevLevel.getFirst();
        lowerStart.setLeft(prevLevel.getLast());
        pLen = p.getLength();
        double d_req = 2000;
        while (d_req<pLen/2) {
            double dMaxBound = (1-t)*2*s/Math.PI*d_req;
            prevNode = null;
            LinkedList<InterferenceNode> curLevel = new LinkedList<>();
            LinkedList<Edge>curEdges = new LinkedList<>();
            InterferenceNode curLowerLeft = prevLevel.getFirst();
            InterferenceNode curLowerRight = null;
            double curL = 0;
            for (InterferenceNode in : prevLevel) {
                curL += in.e.getLength();
                if (curL-curLowerLeft.e.edges.getFirst().getLength()/2-in.e.edges.getLast().getLength()/2>dMaxBound) {
                    InterferenceNode in2 = new InterferenceNode(new ApproximateEdge(curEdges, curLowerLeft.e.p_min-curLowerLeft.e.edges.getFirst().getLength()/2));
                    curEdges.clear();
                    if (prevNode!=null) in2.setLeft(prevNode);
                    prevNode = in2;
                    in2.setLowerLeft(curLowerLeft);
                    curLowerLeft = in;
                    in2.setLowerRight(curLowerRight);
                    curLevel.add(in2);
                    curL = in.e.getLength();
                }
                curEdges.addAll(in.e.edges);
                curLowerRight = in;
            }
            InterferenceNode in2 = new InterferenceNode(new ApproximateEdge(curEdges, curLowerLeft.e.p_min-curEdges.getFirst().getLength()/2));
            in2.setLeft(prevNode);
            in2.setLowerLeft(curLowerLeft);
            in2.setLowerRight(curLowerRight);
            curLevel.add(in2);
            curLevel.getFirst().setLeft(curLevel.getLast());
            prevLevel = curLevel;
            d_req *= 2;
        }
    }
    private class InterferenceNode {
        private InterferenceNode upper_right = null;
        private InterferenceNode upper_left = null;
        private InterferenceNode left = null;
        private InterferenceNode right = null;
        private InterferenceNode lower_left = null;
        private InterferenceNode lower_right = null;
        private ApproximateEdge e;
        public InterferenceNode(ApproximateEdge e) {
            this.e = e;
        }
        public void setLeft(InterferenceNode l) {
            this.left = l;
            l.right = this;
        }
        public void setLowerLeft(InterferenceNode l) {
            this.lower_left = l;
            l.upper_right = this;
        }
        public void setLowerRight(InterferenceNode l) {
            this.lower_right = l;
            l.upper_left = this;
        }
    }
    private class ApproximateEdge extends Edge {
        private final LinkedList<Edge> edges;
        private final double p_min;
        private final double p_max;
        private final double d_max;
        public ApproximateEdge(LinkedList<Edge> edges, double pos) {
            this.edges = new LinkedList<>(edges);
            // start & stop
            setStart(edges.getFirst().getStart());
            setStop(edges.getLast().getStop());
            // dMax
            double d_max = 0;
            Node center = new SimpleNode((getStart().getLat()+getStop().getLat())/2, (getStart().getLon()+getStop().getLon())/2);
            for (Edge e: edges) d_max = Math.max(d_max, dc.getDistance(e, center));
            this.d_max = d_max;
            // length
            double l = 0;
            for (Edge e: edges) l += e.getLength();
            setLength(l);
            // p_min and p_max
            p_min = pos+edges.getFirst().getLength()/2;
            p_max = pos+l-edges.getLast().getLength()/2;
        }
        public ApproximateEdge(Edge e, double pos) {
            edges = new LinkedList<>();
            edges.add(e);
            setStart(e.getStart());
            setStop(e.getStop());
            setLength(e.getLength());
            p_min = pos+e.getLength()/2;
            p_max = p_min;
            d_max = 0;
        }
    }

    double getInterference() {
        InterferenceNode cur = lowerStart;
        boolean first = true;
        double interference = 0;
        while (first || cur!=lowerStart) {
            first = false;
            interference += getInterferenceFromNode(cur);
            cur = cur.right;
        }
        return  interference;
    }
    double getInterferenceFromNode(InterferenceNode in) {
        double interference = 0;
        double d_req = 0;
        InterferenceNode cur = in;
        while (cur != lowerStart) {
            cur = cur.left;
            double pDist = Math.min(in.e.p_min-cur.e.p_max, pLen-in.e.p_max+cur.e.p_min);
            while(((d_req==0 && pDist>=2000) || (d_req>=2000 && pDist>=2*d_req)) && cur.upper_left!=null) {
                cur = cur.upper_left;
                d_req = d_req==0? 2000 : d_req*2;
                pDist = Math.min(in.e.p_min-cur.e.p_max, pLen-in.e.p_max+cur.e.p_min);
            }
            while(pDist<d_req) {
                cur = cur.lower_right;
                d_req = d_req==2000? 0 : d_req/2;
                pDist = Math.min(in.e.p_min-cur.e.p_max, pLen-in.e.p_max+cur.e.p_min);
                if (cur == lowerStart.left) return interference;
            }
            double d_exp = 2*strictness/Math.PI*pDist;
            double d_exp_dec = d_exp-cur.e.d_max;
            double d2 = dc.getDistance2(in.e, cur.e);
            if (d2<d_exp_dec*d_exp_dec) {
                double d = dc.getDistance(in.e, cur.e) + cur.e.d_max;
                if (d<d_exp) interference += (d_exp-d)/d_exp*(cur.e.getLength()*in.e.getLength());
            }
        }
        return interference;
    }
}
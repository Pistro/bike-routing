package routing.algorithms.exact;

import routing.algorithms.heuristics.DistanceCalculator;
import routing.graph.Node;

/**
 * Created by piete on 30/08/2017.
 */
public class MetricDistance extends DistanceCalculator {
    public MetricDistance() {
        super(1, 1);
    }
}

package template;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.HashMap;

public class TopologyStats {
    private static TopologyStats instance = null;
    private Topology topology;
    private static HashMap<City, Double> minDistMap = new HashMap<City, Double>();
    private static HashMap<City, Double> maxDistMap = new HashMap<City, Double>();
    private static double topologyMinDist = Double.POSITIVE_INFINITY;
    public TopologyStats(Topology topology) {
        this.topology = topology;
        this.calcStats();
    }

    public static TopologyStats getInstance(Topology topology) {
        if (instance == null) {
            instance = new TopologyStats(topology);
        }
        return instance;
    }

    public static TopologyStats getInstance() {
        return instance;
    }

    public void calcStats(){
        double minval;
        double maxval;
        for (City city: this.topology.cities()){
            minval = Double.POSITIVE_INFINITY;
            maxval = Double.NEGATIVE_INFINITY;
            for (City subcity: city.neighbors()){
                if (topologyMinDist> city.distanceTo(subcity)) {
                    topologyMinDist = city.distanceTo(subcity);
                }
                if (minval> city.distanceTo(subcity)){
                    minval = city.distanceTo(subcity);
                }
                if (maxval< city.distanceTo(subcity)){
                    maxval = city.distanceTo(subcity);
                }
            }
            this.minDistMap.put(city, minval);
            this.maxDistMap.put(city, maxval);
        }
    }

    public static double getTopologyMinDist() {
        return topologyMinDist;
    }

    public HashMap<City, Double> getMinDistMap() {
        return minDistMap;
    }

    public HashMap<City, Double> getMaxDistMap() {
        return maxDistMap;
    }
}

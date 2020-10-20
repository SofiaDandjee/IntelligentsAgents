package template;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;

public class State {

    private Vehicle vehicle;
    private Topology.City agentCity;
    private List<Task> carried;
    private List<Task> unhandled;
    private double init_cost;
    private List<Action> actions;
    private TopologyStats topologyStats;

    public State(Topology.City c, Vehicle v, List<Task> carried_ , List<Task> unhandled_, List<Action> ac, double initial_cost) {
        this.agentCity = c;
        this.vehicle = v;
        this.carried = new ArrayList<>(carried_);
        this.unhandled = new ArrayList<>(unhandled_);
        this.actions = ac;
        this.init_cost = initial_cost;
    }

    public boolean equals(State s) {
        return this.agentCity.equals(s.getAgentCity()) && this.carried.equals(s.getCarried()) && this.unhandled.equals(s.getUnhandled());
    }

    public List<Task> getCarried() {
        return carried;
    }

    public List<Task> getUnhandled() {
        return unhandled;
    }

    public List<Action> getActions() {
        return actions;
    }


    public boolean isFinal() {
        return unhandled.isEmpty() && carried.isEmpty();
    }

//    public State(State s) {
//        this.agentCity = s.agentCity;
//        this.unhandled = s.unhandled;
//        this.carried = s.carried;
//        this.actions = s.actions;
//        this.init_cost = s.init_cost;
//    }

    public ArrayList<State> getReachableStates() {
        ArrayList<State> children = new ArrayList<>();
        State pickUpState;
        // Deliver && deliver and move to neighbours (only one because we automatically deliver every package)
        State deliverState = deliver();
        if (deliverState != null) {
            children.add(deliverState);
            for (Topology.City city : agentCity.neighbors()) {
                children.add(deliverState.move(city));
            }
            pickUpState = deliverState.pickUp();
        } else {
            pickUpState = pickUp();
            for (Topology.City n : agentCity.neighbors()) {
                children.add(move(n));
            }
        }

        //PickUp as much tasks as he can and move to neighbours
        //State pickUpState = pickUp();
        if (pickUpState != null) {
            for (Topology.City city : agentCity.neighbors()) {
                children.add(pickUpState.move(city));
            }
        }

        return children;
    }

    public State deliver() {
        List<Task> newCarried = new ArrayList<Task>(this.carried);
        List<Action> newActions = new ArrayList<Action>(this.actions);
        if (canDeliver().isEmpty()) {
            return null;
        } else {
            for (Task task : canDeliver()) {
                newCarried.remove(task);
                newActions.add(new Action.Delivery(task));
            }
            return new State(this.agentCity, this.vehicle, newCarried, this.unhandled, newActions, this.init_cost);
        }

    }

    public State move(Topology.City c) {
        List<Action> newActions = new ArrayList<Action>(this.actions);
        newActions.add(new Action.Move(c));
        return new State(c, this.vehicle, this.carried, this.unhandled, newActions, this.init_cost+this.agentCity.distanceTo(c) );
    }

    public State pickUp() {
        List<Task> newUnhandled = new ArrayList<Task>(this.unhandled);
        List<Task> newCarried = new ArrayList<Task>(this.carried);
        List<Action> newActions = new ArrayList<Action>(this.actions);
        int canCarry = vehicle.capacity() - weightCarried();
        if (canPickUp().isEmpty()) {
            return null;
        } else {
            for (Task task : canPickUp()) {
                if (canCarry >= task.weight) {
                    newUnhandled.remove(task);
                    newCarried.add(task);
                    canCarry -= task.weight;
                    newActions.add(new Action.Pickup(task));
                }
            }
            return new State(this.agentCity, this.vehicle, newCarried, newUnhandled, newActions, this.init_cost);
        }
    }

    public int weightCarried() {
        int weight = 0;
        if (!carried.isEmpty()) {
            for (Task task : carried) {
                weight += task.weight;
            }
        }
        return weight;
    }

    public List<Task> canPickUp() {
        List<Task> canPickUp = new ArrayList<>();
        for (Task task : this.unhandled) {
            if (task.pickupCity.equals(agentCity)) {
                canPickUp.add(task);
            }
        }
        return canPickUp;
    }

    public List<Task> canDeliver() {
        List<Task> canDeliver = new ArrayList<>();
        for (Task task : this.carried) {
            if (task.deliveryCity.equals(agentCity)) {
                canDeliver.add(task);
            }
        }
        return canDeliver;
    }

    public Topology.City getAgentCity() {
        return agentCity;
    }

    public double getEstimatedCostNaive() {
        double estCost = this.unhandled.size()*40; // minimum cost
        return estCost;
    }
    public double getEstimatedCost_twoStepHorizon() {
        double estCost = this.unhandled.size()*40;
        List<State> state_list = new ArrayList<State>();
        state_list.add(this);
        for(State st: state_list) {
            List<State> ss = new ArrayList<State>(st.getReachableStates());

            for(State s: ss) {
                if(s.isFinal()) {
                    return new Plan(vehicle.getCurrentCity(), this.actions).totalDistance() * this.vehicle.costPerKm();
                }
            }
        }
        return this.unhandled.size()*40;
    }

    public double getEstimatedCost_basedOnMaxDistance() {
        HashMap<Topology.City, Double> minDistMap = TopologyStats.getInstance().getMinDistMap();
        double estCost = 0.0;
        for (Task task : this.unhandled) {
            double dist = task.pickupCity.distanceTo(task.deliveryCity);
            estCost = Math.max(dist, estCost);
        }
        for (Task taskcarrying: this.carried){
            estCost += minDistMap.get(taskcarrying.deliveryCity);
        }
        return estCost;
    }
    public double getEstimatedCost_withMinAverage() {
        HashMap<Topology.City, Double> minDistMap = TopologyStats.getInstance().getMinDistMap();
        double estCost = 0;

        for (Task taskremain: this.unhandled){
            estCost += minDistMap.get(taskremain.deliveryCity)/2;
            estCost += minDistMap.get(taskremain.pickupCity)/2;
        }
        for (Task taskcarrying: this.carried){
            estCost += minDistMap.get(taskcarrying.deliveryCity);
        }
        return estCost;
    }
    public double getEstimatedCost() {
//        return getEstimatedCostNaive(); // 6941 iterations
//        return getEstimatedCost_twoStepHorizon(); // 12867 iterations
//        return getEstimatedCost_basedOnMaxDistance(); // 1653 iterations
        return getEstimatedCost_withMinAverage(); // 3095 iterations
    }

    public double getTotalCost() {
        return init_cost+ this.getEstimatedCost();
    }
}

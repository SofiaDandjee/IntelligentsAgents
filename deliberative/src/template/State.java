package template;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class State {

    private Vehicle vehicle;
    private Topology.City agentCity;
    private List<Task> carried;
    private List<Task> unhandled;
    private double init_cost;
    private List<Action> actions;


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

    State(Topology.City c, Vehicle v, List<Task> carried_ , List<Task> unhandled_, List<Action> ac, double initial_cost) {
        agentCity = c;
        vehicle = v;
        carried = new ArrayList<>(carried_);
        unhandled = new ArrayList<>(unhandled_);
        actions = ac;
        init_cost = initial_cost;
    }

    public boolean isFinal() {
        return unhandled.isEmpty() && carried.isEmpty();
    }

    public State(State s) {
        this.agentCity = s.agentCity;
        this.unhandled = s.unhandled;
        this.carried = s.carried;
        this.actions = s.actions;
        this.init_cost = s.init_cost;
    }

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

    public double getEstimatedCost() {
        double estCost = this.unhandled.size()*50;
        return estCost;
    }
    public double getTotalCost() {
        return init_cost+ this.getEstimatedCost();
    }
}

package template;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.List;

public class State {

    private Vehicle vehicle;
    private Topology.City agentCity;
    private List<Task> carried;
    private List<Task> unhandled;

    private List<Action> actions;
    private Plan plan;

    State(Topology.City c, Vehicle v, List<Task> carried_ , List<Task> unhandled_, List<Action> actions_, Plan p) {
        agentCity = c;
        vehicle = v;
        carried = new ArrayList<>(carried_);
        unhandled = new ArrayList<>(unhandled_);
        actions = new ArrayList<>(actions_);
        plan = p;
    }

    boolean isFinal() {
        return unhandled.isEmpty() && carried.isEmpty();
    }

    List<State> getReachableStates() {
        List<State> children = new ArrayList<>();
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

    State deliver() {
        List<Task> newCarried = this.carried;
        List<Action> newActions = new ArrayList<>();
        Plan newPlan = this.plan;
        for (Task task : canDeliver()) {
                newCarried.remove(task);
                newActions.add(new Action.Delivery(task));
                newPlan.appendDelivery(task);
        }
        if (newActions.isEmpty()) {
            return null;
        } else return new State(this.agentCity, this.vehicle, newCarried, this.unhandled, actions, newPlan);
    }

    State move(Topology.City c) {
        List<Action> newAction = new ArrayList<>();
        newAction.add(new Action.Move(c));
        Plan newPlan = this.plan;
        newPlan.appendMove(c);
        return new State(c, this.vehicle, this.carried, this.unhandled, newAction, newPlan);
    }

    State pickUp() {
        List<Task> newUnhandled = new ArrayList<>(this.unhandled);
        List<Task> newCarried = new ArrayList<>(this.carried);

        ArrayList<Action> newActions = new ArrayList<>();
        Plan newPlan = this.plan;
        int canCarry = vehicle.capacity() - weightCarried();
        for (Task task : canPickUp()) {
            if (canCarry >= task.weight) {
                newUnhandled.remove(task);
                newCarried.add(task);
                canCarry -= task.weight;
                newPlan.appendPickup(task);
                newActions.add(new Action.Pickup(task));
            }
        }

        if (newActions.isEmpty()) {
            return null;
        } else {
            return new State(this.agentCity, this.vehicle, newCarried, newUnhandled, newActions, newPlan);
        }
    }

    int weightCarried() {
        int weight = 0;
        if (!carried.isEmpty()) {
            for (Task task : carried) {
                weight += task.weight;
            }
        }
        return weight;
    }

    Plan getPlan() {
        return plan;
    }

    List<Task> canPickUp() {
        List<Task> canPickUp = new ArrayList<>();
        for (Task task : this.unhandled) {
            if (task.pickupCity.equals(agentCity)) {
                canPickUp.add(task);
            }
        }
        return canPickUp;
    }

    List<Task> canDeliver() {
        List<Task> canDeliver = new ArrayList<>();
        for (Task task : this.carried) {
            if (task.deliveryCity.equals(agentCity)) {
                canDeliver.add(task);
            }
        }
        return canDeliver;
    }

    Topology.City getAgentCity() {
        return agentCity;
    }
}

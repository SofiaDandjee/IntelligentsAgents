package template;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import logist.task.TaskSet;

import java.util.ArrayList;
import java.util.List;

public class State {

    private State parent_state;
    private Vehicle vehicle;
    private Topology.City agentCity;
    private TaskSet carried;
//    private TaskSet delivered;
    private TaskSet unhandled;

//    private List<Action> actions;
//    private Plan plan;

    State(State parent_state,Topology.City c, Vehicle v, TaskSet carried_ , TaskSet unhandled_) {
        this.parent_state = parent_state;
        this.agentCity = c;
        this.vehicle = v;
        this.carried = carried_;
        this.unhandled = unhandled_;
//        this.delivered = delivered_;

    }
//    boolean isEqualState(State otherState){
//        if(this.getAgentCity()==otherState.getAgentCity() && this.getVehicle()== otherState.getVehicle()
//        && this.getCarried()==otherState.getCarried() && this.getUnhandled()==otherState.getUnhandled()){
//            return true;
//        }
//        return false;
//    }
    boolean isFinal() {
        return this.unhandled.isEmpty() && this.carried.isEmpty();
    }

    List<State> getReachableStates2() {
        List<State> children = new ArrayList<>();

        TaskSet deliverableCarried = TaskSet.copyOf(this.carried);
//        TaskSet deliverableTasks = TaskSet.copyOf(this.delivered);
//        for (Task task : canDeliver()) {
////            deliverableCarried.remove(task);
////            deliverableTasks.add(task);
//            carried.remove(task);
//            delivered.add(task);
//        }
//        this.setDelivered(deliverable_tasks);

//        TaskSet canDeliver = TaskSet.noneOf(unhandled);
        boolean miniflag2= false;
        for (Task task : this.carried) {
            if (task.deliveryCity.equals(this.agentCity)) {
                deliverableCarried.remove(task);
//                deliverableTasks.add(task);
                miniflag2 = true;
//                canDeliver.add(task);
            }
        }
//        for (Topology.City city : agentCity.neighbors()) {
//            children.add(new State(this, city, this.vehicle, deliverableCarried, this.unhandled, deliverableTasks));
//        }
        TaskSet newCarried = TaskSet.copyOf(deliverableCarried);
//        if (miniflag2){
//            newCarried = TaskSet.copyOf(deliverableCarried);
//        }

        TaskSet newUnhandled = TaskSet.copyOf(this.unhandled);
//        TaskSet newCarried = TaskSet.copyOf(carried);

        int canCarry = this.vehicle.capacity() - this.weightCarried();
        boolean miniflag = false;
        for (Task task : this.canPickUp()) {
            if (canCarry >= task.weight) {
                newUnhandled.remove(task);
                newCarried.add(task);
                canCarry -= task.weight;
                miniflag = true;
            }
        }
//        if (miniflag2) {}

            for (Topology.City city : agentCity.neighbors()) {
                if(miniflag2)
                    children.add(new State(this, city, this.vehicle, deliverableCarried, this.unhandled));
                else
                    children.add(new State(this, city, this.vehicle, this.carried, this.unhandled));
                if (miniflag) {
                    children.add(new State(this, city, this.vehicle, newCarried, newUnhandled));
                }
            }

        return children;
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

    TaskSet canPickUp() {
        TaskSet canPickUp = TaskSet.noneOf(this.unhandled);//new ArrayList<>();
        for (Task task : this.unhandled) {
            if (task.pickupCity.equals(agentCity)) {
                canPickUp.add(task);
            }
        }
        return canPickUp;
    }

    TaskSet canDeliver() {
        TaskSet canDeliver = TaskSet.noneOf(unhandled);
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

    public Vehicle getVehicle() {
        return vehicle;
    }

    public TaskSet getCarried() {
        return carried;
    }

    public TaskSet getUnhandled() {
        return unhandled;
    }

    public State getParent() {
        return parent_state;
    }

//    public TaskSet getDelivered() {
//        return delivered;
//    }

    public void setCarried(TaskSet carried) {
        this.carried = carried;
    }

//    public void setDelivered(TaskSet delivered) {
//        this.delivered = delivered;
//    }

    public void setUnhandled(TaskSet unhandled) {
        this.unhandled = unhandled;
    }
}

package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.HashMap;
import java.util.List;

public class State {

    HashMap<Vehicle, Task> nextTaskVehicle;
    HashMap<Task, Task> nextTaskTask;
//    HashMap<Task, Integer> time;
    HashMap<Task, Vehicle> vehicle;
    //List<Vehicle> vehicles;
    //List<Task> tasks;

    State (HashMap<Vehicle, Task> ntv, HashMap<Task, Task> ntt, HashMap<Task, Integer> t, HashMap<Task, Vehicle> v) {
        nextTaskTask = ntt;
        nextTaskVehicle = ntv;
//        time = t;
        vehicle = v;
    }

    State(State s) {
        this.nextTaskTask = new HashMap<>(s.nextTaskTask);
        this.nextTaskVehicle = new HashMap<>(s.nextTaskVehicle);
//        this.time = new HashMap<>(s.time);
        this.vehicle = new HashMap<>(s.vehicle);
    }

    Task nextTask(Vehicle v) {
        return nextTaskVehicle.get(v);
    }

    Task nextTask(Task t) {
        return nextTaskTask.get(t);
    }

//    Integer time (Task t) {
//        return time.get(t);
//    }

    Vehicle vehicle (Task t) {
        return vehicle.get(t);
    }

//    void setTime(Task t, int time1) {
//        time.put(t, time1);
//    }

    void setNextTaskforVehicle(Vehicle v, Task t) {
        nextTaskVehicle.put(v, t);
    }

    void setNextTaskforTask(Task t1, Task t2) {
        nextTaskTask.put(t1, t2);
    }

    void setVehicle(Task t, Vehicle v) {
        vehicle.put(t,v);
    }

    double getCost() {
        double cost = 0;
        //print();
        for (Task task : nextTaskTask.keySet()) {
            //System.out.println(nextTaskTask.keySet());
            Task next = nextTask(task);
            if (next != null) {
                cost += (task.deliveryCity.distanceTo(next.pickupCity) + next.pickupCity.distanceTo(next.deliveryCity))*vehicle(task).costPerKm();
            }
        }
        for (Vehicle v : nextTaskVehicle.keySet()) {
            Task next = nextTask(v);
            if (next != null) {
                cost += (v.homeCity().distanceTo(next.pickupCity) + next.pickupCity.distanceTo(next.deliveryCity))*v.costPerKm();
            }
        }
        return cost;
    }

    void print() {

        for (Task t: nextTaskTask.keySet()) {
            System.out.print("Next Task of task " + t.id + t.pickupCity.name + t.deliveryCity.name + " : ");
            if (nextTask(t) == null) {
                System.out.println("null");
            } else {
                System.out.println(nextTask(t).id);
            }
        }

        for (Vehicle v: nextTaskVehicle.keySet()) {
            System.out.print("Next Task of vehicle " + v.id() + "("+v.homeCity().name+ ") : ");
            if (nextTask(v) == null) {
                System.out.println("null");
            } else {
                System.out.println(nextTask(v).id);
            }
        }


    }

}

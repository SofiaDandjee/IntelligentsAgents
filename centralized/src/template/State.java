package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.HashMap;
import java.util.List;

public class State {

    HashMap<Vehicle, Task> nextTaskVehicle;
    HashMap<Task, Task> nextTaskTask;
    HashMap<Task, Integer> time;
    HashMap<Task, Vehicle> vehicle;
    //List<Vehicle> vehicles;
    //List<Task> tasks;

    State (HashMap<Vehicle, Task> ntv, HashMap<Task, Task> ntt, HashMap<Task, Integer> t, HashMap<Task, Vehicle> v) {
        nextTaskTask = ntt;
        nextTaskVehicle = ntv;
        time = t;
        vehicle = v;
    }

    Task nextTask(Vehicle v) {
        return nextTaskVehicle.get(v);
    }

    Task nextTask(Task t) {
        return nextTaskTask.get(t);
    }

    Integer time (Task t) {
        return time.get(t);
    }

    Vehicle vehicle (Vehicle v) {
        return vehicle.get(v);
    }

    void setTime(Task t, int time1) {
        time.put(t, time1);
    }

    void setNextTaskforVehicle(Vehicle v, Task t) {
        nextTaskVehicle.put(v, t);
    }

    void setNextTaskforTask(Task t1, Task t2) {
        nextTaskTask.put(t1, t2);
    }

    void setVehicle(Task t, Vehicle v) {
        vehicle.put(t,v);
    }

}

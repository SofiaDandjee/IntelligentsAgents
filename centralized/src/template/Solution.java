package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Solution {

    HashMap<Vehicle, TaskAnnotated> nextTaskVehicle;
    HashMap<TaskAnnotated, TaskAnnotated> nextTaskTask;
    HashMap<Task, Vehicle> vehicle;

    Solution(HashMap<Vehicle, TaskAnnotated> ntv, HashMap<TaskAnnotated, TaskAnnotated> ntt, HashMap<Task, Vehicle> v) { //, HashMap<Task, Integer> t
        nextTaskTask = ntt;
        nextTaskVehicle = ntv;
//        time = t;
        vehicle = v;
    }

    Solution(Solution s) {
        this.nextTaskTask = new HashMap<>(s.nextTaskTask);
        this.nextTaskVehicle = new HashMap<>(s.nextTaskVehicle);
        this.vehicle = new HashMap<>(s.vehicle);
    }

    TaskAnnotated nextTask(Vehicle v) {
        return nextTaskVehicle.get(v);
    }

    TaskAnnotated removeTaskDelivery(Task ta){

        Vehicle v = vehicle(ta);
        TaskAnnotated deliver = new TaskAnnotated(ta, Planner.Activity.Deliver);

        TaskAnnotated ta2 = nextTask(v);

        TaskAnnotated previous = null;
        if (ta2 == null) { return deliver;}
        while (!ta2.equals(deliver)) {
            previous = ta2;
            ta2 = this.nextTask(ta2);
        }

        this.nextTaskTask.put(previous, this.nextTask(ta2));
        return ta2;
    }

    TaskAnnotated removeTaskPickup(Task ta) {

        Vehicle v = vehicle(ta);
        TaskAnnotated pickup = new TaskAnnotated(ta, Planner.Activity.Pick);
        TaskAnnotated ta2 = nextTask(v);

        if (ta2.equals(pickup)) {
            TaskAnnotated nextPickup = nextTask(ta2);
            while (nextPickup != null && nextPickup.getActivity() != Planner.Activity.Pick) {
                nextPickup = nextTask(nextPickup);
            }
            this.nextTaskVehicle.put(v, nextPickup);
            return ta2;
        }
        TaskAnnotated previous = null;
        while (!ta2.equals(pickup)) {
            previous = ta2;
            ta2 = this.nextTask(ta2);
        }
        this.nextTaskTask.put(previous, this.nextTask(ta2));

        return ta2;
    }


    Integer deliveryIdxDiff(TaskAnnotated ta) {
        int count=1;
        Task t_org = ta.getTask(); // init ta must be a pickup, do additional check if needed
        ta = this.nextTask(ta);
        while (ta.getTask() != t_org) {
            ta = this.nextTask(ta);
            ++ count;
        }
        return count;
    }
    Integer pickupIdx(TaskAnnotated ta, Vehicle v) {
        int count=0;
        Task t_org = ta.getTask(); // init ta must be a delivery, do additional check if needed
        ta = this.nextTask(v);
        while (ta.getTask() != t_org) {
            ta = this.nextTask(ta);
            ++ count;
        }
        return count;
    }
    Boolean enoughVehicleCapacity(Integer initweight, Vehicle v){
        TaskAnnotated tanow = this.nextTask(v);
        int weight = initweight; // ta1.getTask().weight+ta2.getTask().weight;
        while (tanow != null){
            if (tanow.getActivity() == Planner.Activity.Pick) weight += tanow.getTask().weight;
            else weight -= tanow.getTask().weight;
            tanow = this.nextTask(tanow);
            if (weight>v.capacity()) return false;
        }
        return true;
    }
//    Integer remaingVehicleCapacity(TaskAnnotated ta, Vehicle v){
//        TaskAnnotated tanow = this.nextTask(v);
//        int weight = 0;
//        while (tanow != ta){
//            if (tanow.getActivity() == Planner.Activity.Pick) weight += tanow.getTask().weight;
//            else weight -= tanow.getTask().weight;
//            tanow = this.nextTask(tanow);
//        }
//        return v.capacity()-weight;
//    }
    Integer remaingVehicleCapacity(Vehicle v){
        TaskAnnotated tanow = this.nextTask(v);
        int weight = 0;
        while (tanow != null){
            if (tanow.getActivity() == Planner.Activity.Pick) weight += tanow.getTask().weight;
            else weight -= tanow.getTask().weight;
            tanow = this.nextTask(tanow);
        }
        return v.capacity()-weight;
    }
    TaskAnnotated nextTask(TaskAnnotated t) {
        return nextTaskTask.get(t);
    }


    Vehicle vehicle (Task t) {
        return vehicle.get(t);
    }

    void setNextTaskforVehicle(Vehicle v, TaskAnnotated ta) {
        nextTaskVehicle.put(v, ta);
    }

    void setNextTaskforTask(TaskAnnotated ta1, TaskAnnotated ta2) {
        nextTaskTask.put(ta1,ta2);
    }
    void setVehicle(Task t, Vehicle v) {
        vehicle.put(t,v);
    }

    double getCost() {
        double cost = 0;

        for (Vehicle v : nextTaskVehicle.keySet()) {
            TaskAnnotated next = nextTask(v);
            if (next != null) {
                cost += (v.homeCity().distanceTo(next.getTask().pickupCity))*v.costPerKm();
            }
        }
        for (TaskAnnotated taskA : nextTaskTask.keySet()) {
            Task task = taskA.getTask();
            TaskAnnotated nextA = nextTask(taskA);
            if (nextA != null) {
                Task next = nextA.getTask();
                if (taskA.getActivity() == Planner.Activity.Pick){
                    if (nextA.getActivity() == Planner.Activity.Pick)
                        cost += (task.pickupCity.distanceTo(next.pickupCity))*vehicle(task).costPerKm();
                    else
                        cost += (task.pickupCity.distanceTo(next.deliveryCity))*vehicle(task).costPerKm();

                }
                else{
                    if (nextA.getActivity() == Planner.Activity.Pick)
                        cost += (task.deliveryCity.distanceTo(next.pickupCity))*vehicle(task).costPerKm();
                    else
                        cost += (task.deliveryCity.distanceTo(next.deliveryCity))*vehicle(task).costPerKm();
                }
            }
        }
        return cost;
    }

    void print() {

        for (TaskAnnotated ta: nextTaskTask.keySet()) {
            Task t = ta.getTask();
            System.out.print("Next task of task " + t.id + " " + ta.getActivity() + " : ");
            if (nextTask(ta) == null) {
                System.out.println("null");
            } else {
                System.out.println(nextTask(ta).getTask().id + " " + nextTask(ta).getActivity() );
            }
        }

        for (Vehicle v: nextTaskVehicle.keySet()) {
            System.out.print("Next task of vehicle " + v.id() + "("+v.homeCity().name+ ") : ");
            if (nextTask(v) == null) {
                System.out.println("null");
            } else {
                System.out.println(nextTask(v).getTask().id + " " + nextTask(v).getActivity() + " " + nextTask(v).getTask().pickupCity.name);
            }
        }

    }

    public List<TaskAnnotated> actionPlan (Vehicle v) {
        List<TaskAnnotated> plan = new ArrayList<>();

        if (nextTask(v) == null) {
            return null;
        } else {
            TaskAnnotated t = nextTask(v);
            plan.add(t);
            TaskAnnotated next = nextTask(t);
            while (next != null) {
                plan.add(next);
                next = nextTask(next);
            }
        }

        return plan;
    }

    public List<Task> getTasks(Vehicle v) {
        List <Task> list = new ArrayList<>();
        for (Task t: vehicle.keySet()){
            if (vehicle(t) == v) {
                list.add(t);
            }
        }
        return list;
    }

    public boolean isValid() {
        List<Vehicle> vehicles = new ArrayList<>(nextTaskVehicle.keySet());

        for (Vehicle v: vehicles) {

            List<Task> tasks = getTasks(v);

            if (tasks.size() > 0) {
                for (Task t : tasks) {
                    boolean foundPickup = false;
                    boolean foundDeliver = false;
                    boolean goodVehicle = false;
                    if (vehicle(t) == v ) { goodVehicle = true;}
                    TaskAnnotated next = nextTask(v);
                    if (next.getTask() == t) {
                        if (next.isPickup()) {
                            foundPickup = true;
                        } else if (next.isDeliver()){
                            foundDeliver = true;
                        }
                    }

                    while (next != null && (!foundDeliver || !foundPickup)) {
                        if (next.getTask() == t) {
                            if (next.isPickup()) {
                                foundPickup = true;
                            } else if (next.isDeliver())  {
                                foundDeliver = true;
                            }
                        }
                        next = nextTask(next);
                    }

                    if (!foundDeliver || !foundPickup || !goodVehicle) {
                        return false;
                    }
                }
            }
        }

        return true;
    }



}

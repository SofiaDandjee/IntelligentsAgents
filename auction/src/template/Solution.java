package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

import java.util.*;

public class Solution {

    HashMap<Vehicle, TaskAnnotated> nextTaskVehicle;
    HashMap<TaskAnnotated, TaskAnnotated> nextTaskTask;
    HashMap<Task, Vehicle> vehicle;

    Solution() {
        this.nextTaskTask = new HashMap<>();
        this.nextTaskVehicle = new HashMap<>();
        this.vehicle = new HashMap<>();
    }
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

        //System.out.println("Current solution");
        //print();

        Vehicle v = vehicle(ta);
        //System.out.println(v.id());
        TaskAnnotated deliver = new TaskAnnotated(ta, Planner.Activity.Deliver);
        //System.out.println("Task to deliver" + deliver);

        TaskAnnotated ta2 = nextTask(v);
        //System.out.println(ta2);

        TaskAnnotated previous = null;
        if (ta2 == null) { return deliver;}
        while (!ta2.equals(deliver)) {
            previous = ta2;
            ta2 = this.nextTask(ta2);
            //System.out.println(ta2);
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

    double getDistance() {
        double cost = 0;

        for (Vehicle v : nextTaskVehicle.keySet()) {
            TaskAnnotated next = nextTask(v);
            if (next != null) {
                cost += (v.homeCity().distanceTo(next.getTask().pickupCity));
            }
        }
        for (TaskAnnotated taskA : nextTaskTask.keySet()) {
            Task task = taskA.getTask();
            TaskAnnotated nextA = nextTask(taskA);
            if (nextA != null) {
                Task next = nextA.getTask();
                if (taskA.getActivity() == Planner.Activity.Pick){
                    if (nextA.getActivity() == Planner.Activity.Pick)
                        cost += (task.pickupCity.distanceTo(next.pickupCity));
                    else
                        cost += (task.pickupCity.distanceTo(next.deliveryCity));

                }
                else{
                    if (nextA.getActivity() == Planner.Activity.Pick)
                        cost += (task.deliveryCity.distanceTo(next.pickupCity));
                    else
                        cost += (task.deliveryCity.distanceTo(next.deliveryCity));
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

        System.out.println("Cost is " + getCost());
        System.out.println("Distance is " + getDistance());

    }

    public List<TaskAnnotated> actionPlan (Vehicle v) {
        List<TaskAnnotated> plan = new ArrayList<>();

//        print();
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

    void insertTaskNaive(Task task) {
        //give task to random vehicle
        Random rand = new Random();
        List<Vehicle> vehicles = new ArrayList<Vehicle>(nextTaskVehicle.keySet());
        Vehicle v = vehicles.get(rand.nextInt(vehicles.size()));
        setVehicle(task, v);
        TaskAnnotated pickup = new TaskAnnotated(task, Planner.Activity.Pick);
        TaskAnnotated delivery = new TaskAnnotated(task, Planner.Activity.Deliver);
        TaskAnnotated next = nextTask(v);
        if (next == null) {
            setNextTaskforVehicle(v, pickup);
            setNextTaskforTask(pickup, delivery);
        } else {
            List<TaskAnnotated> chain = new ArrayList<>();
            chain.add(next);
            TaskAnnotated previous = next;
            while (previous != null) {
                next = nextTask(previous);
                previous = next;
                if (previous != null) { chain.add(previous);}
;            }
            setNextTaskforTask(chain.get(chain.size()-1), pickup);
            setNextTaskforTask(pickup, delivery);
        }

    }

    void insertTask(Task task) {
        //give task to random vehicle

        List<Vehicle> vehicles = new ArrayList<Vehicle>(nextTaskVehicle.keySet());
//        Vehicle v = vehicles.get(rand.nextInt(vehicles.size()));
        TaskAnnotated pickup = new TaskAnnotated(task, Planner.Activity.Pick);
        TaskAnnotated delivery = new TaskAnnotated(task, Planner.Activity.Deliver);

        // insert task pick up and delivery if vehicle home city is task pickup city
        for (Vehicle v : vehicles) {
            if (v.homeCity() == task.pickupCity) {
                if (actionPlan(v) == null) {
                    setNextTaskforVehicle(v, pickup);
                } else {
                    if (nextTask(v).getTask().pickupCity != v.homeCity()) {
                        setNextTaskforTask(delivery, nextTask(v));
                        setNextTaskforVehicle(v, pickup);
                    } else {
                        setNextTaskforTask(delivery, nextTask(nextTask(v)));
                        setNextTaskforTask(nextTask(v), pickup);
                    }
                }
                setVehicle(task, v);
                setNextTaskforTask(pickup, delivery);
                return;

            }
        }
        TaskAnnotated insertAfter = null;

        //insert task pickup and delivery after a delivery that is closest to task pickup city
        double shortest = Double.POSITIVE_INFINITY;
        for (Vehicle v : vehicles) {
            List<TaskAnnotated> plan = actionPlan(v);
            if (plan != null) {
                for (TaskAnnotated ta : plan) {
                    if (ta.getTask().deliveryCity.distanceTo(task.pickupCity) < shortest && ta.getActivity() == Planner.Activity.Deliver) {
                        shortest = ta.getTask().deliveryCity.distanceTo(task.pickupCity);
                        insertAfter = ta;
                    }
                }
            }
        }

        Vehicle v = vehicle(insertAfter.getTask());
        setVehicle(task, v);

        setNextTaskforTask(delivery, nextTask(insertAfter));
        setNextTaskforTask(insertAfter, pickup);
        setNextTaskforTask(pickup, delivery);

    }


}

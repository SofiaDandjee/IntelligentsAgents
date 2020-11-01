package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Planner {

    List<Vehicle> vehicles;
    List<Task> tasks;
    public enum Activity{Pick,Deliver}

    Planner (List<Vehicle> v, List<Task> t) {
        this.vehicles = v;
        this.tasks = t;
    }
    State selectInitialSolutionNaive() {


        // get vehicle with max capacity
        int biggest = getIdBiggestVehicle();

        // vehicle is the biggest one for all tasks
        HashMap<Task, Vehicle> taskToVehicle = new HashMap<>();

        for (Task t : tasks) {
            if (t.weight > vehicles.get(biggest).capacity()) {
                // if there is task whose weight is bigger than biggest vehicle capacity: no solution
                System.out.println("No solution can be found. Task with weight higher than capacity");
                System.exit(1);
            }
            taskToVehicle.put(t, vehicles.get(biggest));
        }


        HashMap<TaskAnnotated, TaskAnnotated> taskToTask = new HashMap<>();
        TaskAnnotated tanew = new TaskAnnotated(tasks.get(0), Activity.Pick);

        HashMap<Vehicle, TaskAnnotated> vehicleToTask = new HashMap<>();
        for (Vehicle v : vehicles) {
            if (v.id() == biggest) {
                vehicleToTask.put(v, tanew);
            } else {
                vehicleToTask.put(v, null);
            }
        }


        for (Task t : tasks) {
            TaskAnnotated tanext;
            TaskAnnotated tanextdel = new TaskAnnotated(tasks.get(t.id), Activity.Deliver);
            if (t.id == tasks.size() - 1) {
                taskToTask.put(tanew, tanextdel);
                taskToTask.put(tanextdel, null);
            } else {
//                tanextdel = new TaskAnnotated(tasks.get(t.id), Activity.Deliver);
                tanext = new TaskAnnotated(tasks.get(t.id + 1), Activity.Pick);
                taskToTask.put(tanew, tanextdel);
                taskToTask.put(tanextdel, tanext);
                tanew = tanext;
            }
        }
        return new State(vehicleToTask, taskToTask, taskToVehicle); // taskToTime
    }

    private int getIdBiggestVehicle() {
        double maxCapacity = Double.NEGATIVE_INFINITY;
        int maxVehicle = 0;
        for (Vehicle v : vehicles) {
            if (v.capacity() > maxCapacity) {
                maxVehicle = v.id();
                maxCapacity = v.capacity();
            }
        }
        return maxVehicle;
    }

    private List<State> chooseNeighbours(State s) {
        List<State> neighbours = new ArrayList<>();
        // get random vehicle that has a task
        Random rand = new Random();

        Vehicle v;
        do {
            v = vehicles.get(rand.nextInt(vehicles.size()));
        } while (s.nextTask(v) == null);
        // Applying the changing vehicle operator

        for (Vehicle vj : vehicles) {

            if (v != vj) {

                Task t = s.nextTask(v).getTask();

                if (t.weight <= s.remaingVehicleCapacity(vj)) {
//                if (t.weight <= vj.capacity()) { // task change always happen with immediate delivery, so no effect to capacity (?)

                    State s1 = changingVehicle(s, v, vj);
                    neighbours.add(s1);
                }
            }

        }

        // Applying the changing task order
        // compute number of tasks and tasks for vehicle
        int n = 0;
        TaskAnnotated ta = s.nextTask(v);
//        Task t = ta.getTask();
        do {
            ta = s.nextTask(ta);
//            t = ta.getTask();
            ++n;
        } while (ta != null);


        if (n > 3) {
            for (int id1 = 1; id1 <= n-1; ++id1) {
                for (int id2 = id1+1; id2 <= n; ++id2) {
                    State s1 = changingTaskOrder(s, v, id1, id2);
                    neighbours.add(s1);
                }
            }
        }

        return neighbours;
    }

    private State changingVehicle(State s, Vehicle v1, Vehicle v2) {

        State s1 = new State(s);
        TaskAnnotated ta_delivery;
        TaskAnnotated ta = s1.nextTaskAnnot(v1);
//        Task t = ta.getTask();
        TaskAnnotated ta2 = s1.nextTaskAnnot(ta);
        if (ta2.getActivity() == Activity.Pick){
            s1.setNextTaskforVehicle(v1, ta2);
            ta_delivery = s1.removeTaskDelivery(ta); // remove delivery from the task chain
        }
        else{
            s1.setNextTaskforVehicle(v1, s1.nextTaskAnnot(ta2));
            ta_delivery = ta2;
        }

        s1.setNextTaskforTask(ta, ta_delivery );
        s1.setNextTaskforTask(ta_delivery, s1.nextTaskAnnot(v2) );
        s1.setNextTaskforVehicle(v2, ta);

        s1.setVehicle(ta.getTask(), v2);

        return s1;
    }

    private State changingTaskOrder(State s, Vehicle v, int id1, int id2) {
        State s1 = new State(s);
        TaskAnnotated task1Prev;
        TaskAnnotated t1;

        int count = 0;

        if (id1 == 1) {
            task1Prev = null;
            t1 = s1.nextTask(v);
            count = 1;
        } else {
            task1Prev = s1.nextTask(v); // previous task of task1
            t1 = s1.nextTask(task1Prev); // task1
            count = 2;
            while (count < id1) {
                task1Prev = t1;
                t1 = s1.nextTask(t1);
                ++ count;
            }
        }

        TaskAnnotated task1Post = s1.nextTask(t1); // task delivered after t1

        TaskAnnotated task2Prev = t1; // previous task of t2
        TaskAnnotated t2 = s1.nextTask(task2Prev); // t2
        ++count;
        while (count < id2) {
            task2Prev = t2;
            t2 = s1.nextTask(t2);
            ++count;
        }
        TaskAnnotated task2Post = s1.nextTask(t2); // task delivered after t2

        Boolean exchangeFlag = false;
        // Logic: its okay to postpone delivery and early pickup (given capacity available), check otherwise for possible delivery before pickup
        if (t1.getActivity() == Planner.Activity.Pick){
            if (t2.getActivity() == Planner.Activity.Pick){
                if(id2<(s1.deliveryIdxDiff(t1)+id1)) exchangeFlag=true;
            }
            else{
                if ((s1.pickupIdx(t2,v)<id1) & (id2<(s1.deliveryIdxDiff(t1)+id1))) exchangeFlag=true;
            }
        }
        else{
            if (t2.getActivity() == Planner.Activity.Pick) {
                int remainCap = s1.remaingVehicleCapacity(t1,v)-t1.getTask().weight-t2.getTask().weight;
                if(remainCap>=0) exchangeFlag=true;
                else
                    exchangeFlag=false;
            }
            else{
                if (s1.pickupIdx(t2,v) < id1) exchangeFlag=true;
            }
        }
        if (exchangeFlag) {
            // exchanging two tasks
            if (task1Post == t2) {
                // the task t2 is delivered immediately after t1
                if (task1Prev == null) {
                    s1.setNextTaskforVehicle(v, t2);
                } else {
                    s1.setNextTaskforTask(task1Prev, t2);
                }

                s1.setNextTaskforTask(t2, t1);
                s1.setNextTaskforTask(t1, task2Post);
            } else {
                if (task1Prev == null) {
                    s1.setNextTaskforVehicle(v, t2);
                } else {
                    s1.setNextTaskforTask(task1Prev, t2);
                }
                //s1.setNextTaskforTask(task1Prev, t2);
                s1.setNextTaskforTask(task2Prev, t1);
                s1.setNextTaskforTask(t2, task1Post);
                s1.setNextTaskforTask(t1, task2Post);
            }
        }

        return s1;
    }

    private State localChoice(List<State> N, State sOld) {
        double minCost = Double.POSITIVE_INFINITY;
        double cost;
        List<State> bestChoices = new ArrayList<>();
        double probThreshold = 0.4;
        for (State neighbour : N) {
            cost = neighbour.getCost();
            if (cost < minCost) {
                minCost = cost;
                bestChoices.add(neighbour);
            }
        }
        if(Math.random() > probThreshold){
            return sOld;
        }
        return bestChoices.get(new Random().nextInt(bestChoices.size()));
    }

    public State SLS() {
        State s = selectInitialSolutionNaive();
        int maxIter = 1000;
        int iter = 0;
        double bestCost = s.getCost();
        State bestState = s;
//        List<Plan> tempPlan;
//        List<Plan> tempPlan2;
        do {
            State sOld = s;
//            tempPlan2 = CentralizedMultiTask.stateToPlan(sOld, this.vehicles);
            List <State> neighbours = chooseNeighbours(sOld);
            s = localChoice(neighbours, sOld);
            if (s.getCost() < bestCost) {
                bestCost = s.getCost();
                bestState = s;
//                tempPlan = CentralizedMultiTask.stateToPlan(bestState, this.vehicles);
                System.out.println("Better solution found!");
            }
            ++iter;

        } while (iter < maxIter);
        return bestState;
    }

}

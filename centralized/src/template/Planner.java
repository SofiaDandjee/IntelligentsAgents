package template;

import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Planner {

    List<Vehicle> vehicles;
    List<Task> tasks;

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

        //tasks are delivered sequentially
        HashMap<Task, Integer> taskToTime = new HashMap<>();
        for (Task t : tasks) {
            taskToTime.put(t, t.id+1);
        }

        HashMap<Task, Task> taskToTask = new HashMap<>();
        for (Task t : tasks) {
            if (t.id == tasks.size() - 1) {
                taskToTask.put(t, null);
            } else {
                taskToTask.put(t, tasks.get(t.id + 1));
            }
        }

        HashMap<Vehicle, Task> vehicleToTask = new HashMap<>();
        for (Vehicle v : vehicles) {
            if (v.id() == biggest) {
                vehicleToTask.put(v, tasks.get(0));
            } else {
                vehicleToTask.put(v, null);
            }
        }


        return new State(vehicleToTask, taskToTask, taskToTime, taskToVehicle);
    }

    private State selectInitialSolutionBetter() {
        //TO DO: create an initial solution where if there is a task at home city of vehicle, pick it up
        HashMap<Task, Vehicle> taskToVehicle = new HashMap<>();
        HashMap<Task, Integer> taskToTime = new HashMap<>();
        HashMap<Task, Task> taskToTask = new HashMap<>();
        HashMap<Vehicle, Task> vehicleToTask = new HashMap<>();

        List<Task> toBeDelivered = tasks;
        for (Task t : toBeDelivered) {
            for (Vehicle v : vehicles) {
                if (t.pickupCity == v.homeCity()) {
                    vehicleToTask.put(v, t);
                    taskToVehicle.put(t,v);
                    taskToTime.put(t, 1);
                    toBeDelivered.remove(t);
                }
            }
        }
        // to continue
        return null;
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

                Task t = s.nextTask(v);

                if (t.weight <= vj.capacity()) {

                    State s1 = changingVehicle(s, v, vj);
                    neighbours.add(s1);
                }
            }

        }

        // Applying the changing task order
        // compute number of tasks and tasks for vehicle
        int n = 0;
        Task t = s.nextTask(v);
        do {
            t = s.nextTask(t);
            ++n;
        } while (t != null);


        if (n >= 2) {
            for (int id1 = 1; id1 <= n-1; ++id1) {
                for (int id2 = id1+1; id2 <= n; ++id2) {
                    State s1 = changingTaskOrder(s, v, id1, id2);
                    neighbours.add(s1);
                }
            }
        }

        return neighbours;
    }

//    private void updateTime(State s, Vehicle v) {
//        Task ti = s.nextTask(v);
//        if (ti != null) {
//            s.setTime(ti, 1);
//            Task tj;
//            do {
//                tj = s.nextTask(ti);
//                if (tj != null) {
//                    s.setTime(tj, s.time(ti) + 1);
//                    ti = tj;
//                }
//            } while (tj != null);
//        }
//    }

    private State changingVehicle(State s, Vehicle v1, Vehicle v2) {

        State s1 = new State(s);

        Task t = s1.nextTask(v1);
        s1.setNextTaskforVehicle(v1, s1.nextTask(t));
        s1.setNextTaskforTask(t, s1.nextTask(v2));
        s1.setNextTaskforVehicle(v2, t);
//        updateTime(s1, v1);
//        updateTime(s1, v2);
        s1.setVehicle(t, v2);

        return s1;
    }

    private State changingTaskOrder(State s, Vehicle v, int id1, int id2) {
        State s1 = new State(s);
        Task task1Prev;
        Task t1;

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

        Task task1Post = s1.nextTask(t1); // task delivered after t1

        Task task2Prev = t1; // previous task of t2
        Task t2 = s1.nextTask(task2Prev); // t2
        ++count;
        while (count < id2) {
            task2Prev = t2;
            t2 = s1.nextTask(t2);
            ++count;
        }
        Task task2Post = s1.nextTask(t2); // task delivered after t2
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
//        updateTime(s1, v);

        return s1;
    }

    private State localChoice(List<State> N, State sOld) {
        double minCost = Double.POSITIVE_INFINITY;
        double cost;
//        State choice = null;
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
        int maxIter = 500;
        int iter = 0;
        double bestCost = s.getCost();
        State bestState = s;
        do {
            State sOld = s;
            List <State> neighbours = chooseNeighbours(sOld);
            s = localChoice(neighbours, sOld);
            if (s.getCost() < bestCost) {
                bestCost = s.getCost();
                bestState = s;
                System.out.println("Better solution found!");
            }
            ++iter;

        } while (iter < maxIter);
        return bestState;
    }

}

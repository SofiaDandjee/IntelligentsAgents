package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;

import javax.sound.midi.SysexMessage;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Planner {

    List<Vehicle> vehicles;
    List<Task> tasks;
    List<Solution> solutions;
    Solution bestSolution;
    Double bestCost;
    long startTime;
    long timeout;

    public enum Activity{Pick,Deliver}

    Planner(Planner plan) {
        this.vehicles = new ArrayList<>(plan.vehicles);
        this.tasks = new ArrayList<>(plan.tasks);
        this.solutions = new ArrayList<>();
        this.bestCost = 0.0;
        this.bestSolution = null;
        this.timeout = plan.timeout;
        this.startTime = plan.startTime;
    }
    Planner (List<Vehicle> v) {
        this.vehicles = new ArrayList<>(v);
        this.tasks = new ArrayList<>();
        this.bestCost = 0.0;
        this.bestSolution = null;
        this.solutions = new ArrayList<>();
        //this.timeout = null;
        //this.startTime = null;
    }

    Planner (List<Vehicle> v, List<Task> t) {
        this.vehicles = new ArrayList<>(v);
        this.tasks = new ArrayList<>(t);
        this.solutions = new ArrayList<>();
        this.bestSolution = null;
        this.bestCost = 0.0;
    }

    Planner (List<Vehicle> v, List<Task> t, long start, long to) {
        this.vehicles = new ArrayList<>(v);
        this.tasks = new ArrayList<>(t);
        this.solutions = new ArrayList<>();
        this.timeout = to;
        this.startTime = start;
    }

    Solution selectInitialSolutionNaive() {
        HashMap<TaskAnnotated, TaskAnnotated> taskToTask = new HashMap<>();
        TaskAnnotated tanew = new TaskAnnotated(tasks.get(0), Activity.Pick);
        HashMap<Vehicle, TaskAnnotated> vehicleToTask = new HashMap<>();
        HashMap<Task, Vehicle> taskToVehicle = new HashMap<>();

        // get vehicle with max capacity
        int biggest = getIdBiggestVehicle();

        for (Task t : tasks) {
            if (t.weight > vehicles.get(biggest).capacity()) {
                // if there is task whose weight is bigger than biggest vehicle capacity: no solution
                System.out.println("No solution can be found. Task with weight higher than capacity");
                System.exit(1);
            }
            taskToVehicle.put(t, vehicles.get(biggest));
        }
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
                tanext = new TaskAnnotated(tasks.get(t.id + 1), Activity.Pick);
                taskToTask.put(tanew, tanextdel);
                taskToTask.put(tanextdel, tanext);
                tanew = tanext;
            }
        }
        return new Solution(vehicleToTask, taskToTask, taskToVehicle);
    }

    Boolean enoughSpace(HashMap<Task, Vehicle> taskToVehicle, Vehicle v, Integer newWeight){
        Integer weight = newWeight;
        for (Task t : tasks) {
            if(taskToVehicle.get(t)==v){
                weight += t.weight;
            }
        }
        return (v.capacity()>=weight);
    }
    Solution selectInitialSolution() {
        HashMap<Task, Vehicle> taskToVehicle = new HashMap<>();
        HashMap<TaskAnnotated, TaskAnnotated> taskToTask = new HashMap<>();
        HashMap<Vehicle, TaskAnnotated> vehicleToTask = new HashMap<>();
        HashMap<Vehicle, TaskAnnotated> vehicleLastTask = new HashMap<>();
        HashMap<Integer, TaskAnnotated> taskToTaskAnnot = new HashMap<>();

        HashMap<Integer, TaskAnnotated> taskToDeliver = new HashMap<>();
        HashMap<Integer, TaskAnnotated> taskToPickup = new HashMap<>();

        Random rand = new Random();

        int biggest = getIdBiggestVehicle();
//        System.out.println(tasks);


        for (Task t : tasks) {
            if (t.weight > vehicles.get(biggest).capacity()) {
                // if there is task whose weight is bigger than biggest vehicle capacity: no solution
                System.out.println("No solution can be found. Task with weight higher than capacity");
                System.exit(1);
            }
            taskToTaskAnnot.put(t.id, new TaskAnnotated(t,Activity.Pick));
            taskToTaskAnnot.put(t.id+tasks.size(), new TaskAnnotated(t,Activity.Deliver));
            taskToPickup.put(t.id, new TaskAnnotated(t,Activity.Pick));
            taskToDeliver.put(t.id, new TaskAnnotated(t,Activity.Deliver));


            for (Vehicle v : vehicles) {
                if ((v.homeCity()==t.pickupCity)&(enoughSpace(taskToVehicle,v,t.weight))){
                    taskToVehicle.put(t,v);
                    TaskAnnotated ta = vehicleToTask.get(v);
                    if(ta == null) {
                        vehicleToTask.put(v, taskToPickup.get(t.id));
                    }
                    else{
                        taskToTask.put(vehicleLastTask.get(v), taskToPickup.get(t.id));
                    }
                    vehicleLastTask.put(v, taskToPickup.get(t.id));
                }
            }
        }


        for (Task t : tasks) {
            Vehicle v = taskToVehicle.get(t);
            if (v != null){
                taskToTask.put(vehicleLastTask.get(v),taskToDeliver.get(t.id));
                vehicleLastTask.put(v, taskToDeliver.get(t.id));
            }
        }

        for (Task t : tasks) {
            // seperate for loop is needed to makesure vehicles have capacity to take new tasks
            Vehicle v = taskToVehicle.get(t);
            if (v == null) {
                do {
                    v = vehicles.get(rand.nextInt(vehicles.size()));
                } while (v.capacity() < t.weight);
                taskToVehicle.put(t, v);
                TaskAnnotated ta = vehicleToTask.get(v);
                if (ta == null) {
                    vehicleToTask.put(v, taskToPickup.get(t.id));
                } else {
                    taskToTask.put(vehicleLastTask.get(v), taskToPickup.get(t.id));
                }
                taskToTask.put(taskToPickup.get(t.id), taskToDeliver.get(t.id));
                vehicleLastTask.put(v, taskToDeliver.get(t.id));
            }
        }

        for (Vehicle v : vehicles) {
            if(vehicleLastTask.get(v)==null) vehicleToTask.put(v,null);
            else taskToTask.put(vehicleLastTask.get(v),null);
        }

        return new Solution(vehicleToTask, taskToTask, taskToVehicle);
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

    private List<Solution> chooseNeighbours(Solution s) {
        List<Solution> neighbours = new ArrayList<>();
        // get random vehicle that has a task
        Random rand = new Random();
        Vehicle v;
        do {
            v = vehicles.get(rand.nextInt(vehicles.size()));
        } while (s.actionPlan(v) == null);

        // Applying the changing vehicle operator
        List<Task> plan = s.getTasks(v);
        for (Vehicle vj : vehicles) {
            if (v != vj) {
                for (Task t : plan) {
                    if (t.weight <= s.remaingVehicleCapacity(vj)) {
                        Solution s1 = changingVehicleBis(s, v, t, vj);
                        neighbours.add(s1);
                    }
                }
            }
        }


        // Applying the changing task order
        do {
            v = vehicles.get(rand.nextInt(vehicles.size()));
        } while (s.actionPlan(v) == null);

        // compute number of task actions for vehicle
        int n = 0;
        TaskAnnotated ta = s.nextTask(v);
        do {
            ta = s.nextTask(ta);
            ++n;
        } while (ta != null);


        if (n > 3) {
            for (int id1 = 1; id1 <= n-1; ++id1) {
                for (int id2 = id1+1; id2 <= n; ++id2) {
                    Solution s1 = changingTaskOrder(s, v, id1, id2);
                    if (s1!=null) {neighbours.add(s1);}
                }
            }
        }
        return neighbours;
    }

    private Solution changingVehicle(Solution s, Vehicle v1, Vehicle v2) {

        Solution s1 = new Solution(s);

        TaskAnnotated ta_delivery;
        TaskAnnotated ta = s1.nextTask(v1);
        TaskAnnotated ta2 = s1.nextTask(ta);
        if (ta2.getActivity() == Activity.Pick){
            s1.setNextTaskforVehicle(v1, ta2);
            ta_delivery = s1.removeTaskDelivery(ta.getTask()); // remove delivery from the task chain
        }
        else{
            s1.setNextTaskforVehicle(v1, s1.nextTask(ta2));
            ta_delivery = ta2;
        }

        s1.setNextTaskforTask(ta, ta_delivery );
        s1.setNextTaskforTask(ta_delivery, s1.nextTask(v2) );
        s1.setNextTaskforVehicle(v2, ta);

        s1.setVehicle(ta.getTask(), v2);
        return s1;
    }

    private Solution changingVehicleBis(Solution s, Vehicle v1, Task ta, Vehicle v2) {

        Solution s1 = new Solution(s);

        TaskAnnotated ta_delivery = s1.removeTaskDelivery(ta);
        if (ta_delivery == null) { System.out.println("delivery null");}

        TaskAnnotated ta_pickup = s1.removeTaskPickup(ta);
        if (ta_pickup == null) { System.out.println("pickup null");}
        s1.setNextTaskforTask(ta_pickup, ta_delivery );
        s1.setNextTaskforTask(ta_delivery, s1.nextTask(v2));
        s1.setNextTaskforVehicle(v2, ta_pickup);
        s1.setVehicle(ta, v2);

        return s1;
    }

    private Solution changingTaskOrder(Solution s, Vehicle v, int id1, int id2) {
        Solution s1 = new Solution(s);
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
        // Logic: its okay to postpone delivery and do an early pickup (given capacity available),
        // otherwise check for possibility of delivery before pickup
        if(t1.getTask() != t2.getTask()) {
            // cannot exchange pickup and delivery order of same task
            if (t1.isPickup()) {
                if (t2.isPickup()) {
                    if ((id2 < (s1.deliveryIdxDiff(t1) + id1)) & s1.enoughVehicleCapacity((-t1.getTask().weight + t2.getTask().weight), v))
                        exchangeFlag = true;
                } else {
                    if ((s1.pickupIdx(t2, v) < id1) & (id2 < (s1.deliveryIdxDiff(t1) + id1)))
                        exchangeFlag = true; // no need to check for the weight constraints coz going to deliver early
                }
            } else {
                if (t2.isPickup()) {
                    if (s1.enoughVehicleCapacity((t1.getTask().weight + t2.getTask().weight), v)) exchangeFlag = true;

                } else {
                    if ((s1.pickupIdx(t2, v) < id1) & s1.enoughVehicleCapacity((t1.getTask().weight - t2.getTask().weight), v))
                        exchangeFlag = true;
                }
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
            return s1;
        } else return null;

    }

    private Solution localChoice(List<Solution> neighbours, Solution sOld, int iter) {
        double minCost = Double.MAX_VALUE;
        double cost;
        double probThreshold = 0.5;
        Random rand = new Random();
        List<Solution> bestChoices = new ArrayList<>();
        List<Solution> bestPossibleChoices = new ArrayList<>();

        //System.out.println(neighbours.size());
        for (Solution n : neighbours) {
            cost = n.getCost();
            if (cost < minCost) {
                minCost = cost;
                bestPossibleChoices.add(n);
                if (cost<bestCost){
                    //We keep the best solution even if we discard it during the search
                    bestSolution = n;
                    bestCost = cost;
                    //System.out.println(bestCost);
                }
            }
        }

        // we discard the new best with probability 1-p
        if(rand.nextInt() > probThreshold){
            return sOld;
        }
        // else we output a random Solution with min cost
        for (Solution bestn: bestPossibleChoices){
            if (bestn.getCost() == minCost) bestChoices.add(bestn);
        }
        Integer a = rand.nextInt(bestChoices.size());
        return bestChoices.get(a);

    }

    public Solution SLS() {
        Solution s = selectInitialSolution();
        if (!s.isValid()) {
            System.out.println("init sol not valid");
        s.print();}

        bestSolution = s;
        bestCost= s.getCost();
        int maxIter = 2000;
        int iter = 0;
        do {
            Solution sOld = s;
            List<Solution> neighbours = chooseNeighbours(sOld);
            s = localChoice(neighbours, sOld, iter);
            ++iter;

        //} while (System.currentTimeMillis() - this.startTime < this.timeout - 2000);
        } while(iter < maxIter);
        return  bestSolution;
    }

    public Solution search(Solution init) {

        Solution s = init;
        if (!s.isValid()) {
            System.out.println("init sol not valid");
            s.print();}

        bestSolution = s;
        bestCost= s.getCost();
        int maxIter = 10000;
        int iter = 0;
        do {
            Solution sOld = s;
            List<Solution> neighbours = chooseNeighbours(sOld);
            s = localChoice(neighbours, sOld, iter);
            ++iter;
            //} while (System.currentTimeMillis() - this.startTime < this.timeout - 2000);
        } while(iter < maxIter);
        return  bestSolution;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void addTask(Task t) {
        tasks.add(t);
    }

    public Solution getBestSolution() {
        return bestSolution;
    }

    public Double getBestCost () {
        return bestCost;
    }
}

package template;

import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.HashMap;
import java.util.List;

public class Planner {

    List<Vehicle> vehicles;
    List<Task> tasks;

    State selectInitialSolution() {
        // get vehicle with max capacity
        int biggest = getIdBiggestVehicle();

        // vehicle is the biggest one for all tasks
        HashMap<Task, Vehicle> taskToVehicle = new HashMap<>();
        for (Task t : tasks) {
            if (t.weight > vehicles.get(biggest).capacity()) {
                // if there is task whose weight is bigger than biggest vehicle capacity
                // no solution
                return null;
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

    /*private int chooseNeighbours() {

    }*/

    private void updateTime(State s, Vehicle v) {
        Task ti = s.nextTask(v);
        if (ti != null) {
            s.setTime(ti, 1);
            Task tj;
            do {
                tj = s.nextTask(ti);
                if (tj != null) {
                    s.setTime(tj, s.time(ti) + 1);
                    ti = tj;
                }
            } while (tj != null);
        }
    }

    private State changingVehicle(State s, Vehicle v1, Vehicle v2) {
        State s1 = s;
        Task t = s.nextTask(v1);
        s1.setNextTaskforVehicle(v1, s1.nextTask(t));
        s1.setNextTaskforTask(t, s1.nextTask(v2));
        s1.setNextTaskforVehicle(v2, t);
        updateTime(s1, v1);
        updateTime(s1, v2);
        s1.setVehicle(t, v2);
        return s1;
    }

}

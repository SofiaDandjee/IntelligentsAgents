package template;

//the list of imports

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedSingleTask implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;


    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;

    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();

        PlannerSingleTask planner;
        planner = new PlannerSingleTask(agent.vehicles(), new ArrayList<>(tasks));

        StateSingleTask solution = planner.SLS();
        solution.print();
        List<Plan> plans = stateToPlan(solution, vehicles);
        System.out.println(plans);

//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        /**Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

        List<Plan> plans = new ArrayList<Plan>();
        plans.add(planVehicle1);
        while (plans.size() < vehicles.size()) {
            plans.add(Plan.EMPTY);
        }*/
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");
        
        return plans;
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }



    public List<Plan> stateToPlan(StateSingleTask s, List<Vehicle> vehicles) {

        List <Plan> plans = new ArrayList<>();

        for (Vehicle v : vehicles) {
            // create the plan for each vehicle
            Plan plan = new Plan(v.homeCity());
            // first go to pickup city of first task
            Task current = s.nextTask(v);
            if (current != null) {
                List<City> path = v.homeCity().pathTo(current.pickupCity);
                for (City c : path) {
                    plan.appendMove(c);
                }

                plan.appendPickup(current);

                path = current.pickupCity.pathTo(current.deliveryCity);
                for (City c : path) {
                    plan.appendMove(c);
                }
                plan.appendDelivery(current);

                // then all following tasks
                Task next = s.nextTask(current);
                while (next != null) {
                    path = current.deliveryCity.pathTo(next.pickupCity);
                    for (City c : path) {
                        plan.appendMove(c);
                    }
                    plan.appendPickup(next);

                    path = next.pickupCity.pathTo(next.deliveryCity);
                    for (City c : path) {
                        plan.appendMove(c);
                    }
                    plan.appendDelivery(next);
                    current = next;
                    next = s.nextTask(next);
                }
            }

            plans.add(plan);

        }

        return plans;
    }




}

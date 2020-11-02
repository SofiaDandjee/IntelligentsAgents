package template;

//the list of imports

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedMultiTask implements CentralizedBehavior {

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

        Planner planner;
        planner = new Planner(agent.vehicles(), new ArrayList<>(tasks));

        Solution solution = null;

        solution = planner.SLS();

        System.out.println(solution.getCost());
        List<Plan> plans = stateToPlan(solution, vehicles);
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");
        
        return plans;
    }



    public static List<Plan> stateToPlan(Solution s, List<Vehicle> vehicles) {

        List <Plan> plans = new ArrayList<>();

        for (Vehicle v : vehicles) {
            // create the plan for each vehicle
            Plan plan = new Plan(v.homeCity());
            // first go to pickup city of first task
            TaskAnnotated current = s.nextTask(v);
            if (current != null) {
                List<City> path = v.homeCity().pathTo(current.getTask().pickupCity);
                for (City c : path) {
                    plan.appendMove(c);
                }
                plan.appendPickup(current.getTask());
                TaskAnnotated nextA = s.nextTask(current);
                while (nextA != null) {
                    if (current.getActivity() == Planner.Activity.Pick){
                        if (nextA.getActivity() == Planner.Activity.Pick){
                            path = current.getTask().pickupCity.pathTo(nextA.getTask().pickupCity);
                            for (City c : path) {
                                plan.appendMove(c);
                            }
                            plan.appendPickup(nextA.getTask());
                        }
                        else{
                            path = current.getTask().pickupCity.pathTo(nextA.getTask().deliveryCity);
                            for (City c : path) {
                                plan.appendMove(c);
                            }
                            plan.appendDelivery(nextA.getTask());
                        }
                    }
                    else{
                        if (nextA.getActivity() == Planner.Activity.Pick){
                            path = current.getTask().deliveryCity.pathTo(nextA.getTask().pickupCity);
                            for (City c : path) {
                                plan.appendMove(c);
                            }
                            plan.appendPickup(nextA.getTask());
                        }
                        else{
                            path = current.getTask().deliveryCity.pathTo(nextA.getTask().deliveryCity);
                            for (City c : path) {
                                plan.appendMove(c);
                            }
                            plan.appendDelivery(nextA.getTask());
                        }
                    }
                    current = nextA;
                    nextA = s.nextTask(nextA);
                }
            }
            plans.add(plan);
        }
        return plans;
    }




}

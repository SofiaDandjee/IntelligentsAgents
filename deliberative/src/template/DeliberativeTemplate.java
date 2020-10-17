package template;

/* import table */
import com.sun.source.doctree.SeeTree;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}


	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		List<Task> t = new ArrayList<Task>(tasks);
		List<Task> carried = new ArrayList<>(vehicle.getCurrentTasks());
		State init = new State(current, vehicle, carried, t, Collections.<Action>emptyList(), plan);

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			System.out.println("BFS ALGORITHM");
			plan = BFS(init, vehicle);
			//System.out.println("Cost of optimal plan");
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}

	private Plan BFS(State initialNode, Vehicle vehicle) {

		//State Q = initialNode;
		List<State> toVisit = new ArrayList<State>();
		toVisit.add(initialNode);
		List<State> visited = new ArrayList<>();
		State optimalState = initialNode;
		double optimalCost = Double.POSITIVE_INFINITY;
		int numVisited = 0;
		while (!toVisit.isEmpty()) {
			State current = toVisit.get(0);
			toVisit.remove(current);
			if (current.isFinal() && current.getPlan().totalDistance()*vehicle.costPerKm() < optimalCost) {
				System.out.println("Final node encountered!");
				optimalCost = current.getPlan().totalDistance()*vehicle.costPerKm();
				optimalState = current;
			}
			if (!visited.contains(current)) {
				visited.add(current);
				List<State> children = current.getReachableStates();
				toVisit.addAll(children);
				++numVisited;
				System.out.println(numVisited);
			}

		}
		System.out.println("OPTIMAL PLAN FOUND WITH"+ numVisited+ "ITERATIONS");
		return optimalState.getPlan();
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}

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

		ArrayList<Task> t = new ArrayList<Task>(tasks);
		ArrayList<Task> carried = new ArrayList<Task>(vehicle.getCurrentTasks());
		State init = new State(current, vehicle, carried, t, Collections.<Action>emptyList(), 0.);
		State found = null;
		long start = System.currentTimeMillis();
		// Compute the plan with the selected algorithm.
		long end;
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = ASTAR(init, vehicle);
			end = System.currentTimeMillis();
			break;
		case BFS:
			// ...
			System.out.println("BFS ALGORITHM");
			plan = BFS(init, vehicle);
			end = System.currentTimeMillis();
			break;
		default:
			throw new AssertionError("Should not happen.");
		}

		//List<Action> finalActions = found.getActions();
		//Plan finalPlan = new Plan(vehicle.getCurrentCity(), finalActions);
		System.out.println("Computation time : " + (end-start)/1000 + " seconds.");
		System.out.println("Optimal cost : " + plan.totalDistance()*vehicle.costPerKm());
		return plan;
	}
	private Plan ASTAR(State initialNode, Vehicle vehicle) {


//		Queue<State> toVisit = new LinkedList<State>();
		HashSet<State> visited = new HashSet<State>();
		HashMap<State, Double> toVisitMap = new HashMap<State, Double>();
//		HashMap<State, Double> visitedMap = new HashMap<State, Double>();

//		toVisit.add(initialNode);
		toVisitMap.put(initialNode, 0.);
		double optimalCost = Double.POSITIVE_INFINITY;
		double costCurrent = 0;
		List<Action> actionsCurrent;
//		Plan currentPlan;
//		Plan optimalPlan = new Plan(vehicle.getCurrentCity());
		int numVisited = 0;
		while (!toVisitMap.isEmpty()) {
//			State current = toVisit.remove();
			State current = Collections.min(toVisitMap.entrySet(), (entry1, entry2) -> (int) (entry1.getValue() - entry2.getValue()) ).getKey();
			toVisitMap.remove(current);
			if (current.isFinal()) {
				actionsCurrent = current.getActions();
				return new Plan(vehicle.getCurrentCity(), actionsCurrent);

			}

			boolean alreadyVisited = false;
			for(State s: visited) {
				if(s.equals(current) && (s.getTotalCost()<=current.getTotalCost())) {
					alreadyVisited = true;
					break;
				}
			}
			if(! alreadyVisited) {
				visited.add(current);
				ArrayList<State> children = current.getReachableStates();
				++numVisited;
//				toVisit.addAll(children);
				for (State childstate: children){
					toVisitMap.put(childstate, childstate.getTotalCost());
				}
			}

		}
		System.out.println("OPTIMAL PLAN COULD NOT REACHED!");
		return null;
	}


	private Plan BFS(State initialNode, Vehicle vehicle) {


		Queue<State> toVisit = new LinkedList<State>();
		HashSet<State> visited = new HashSet<State>();

		toVisit.add(initialNode);

		double optimalCost = Double.POSITIVE_INFINITY;
		double costCurrent = 0;
		List<Action> actionsCurrent;
		Plan currentPlan;
		Plan optimalPlan = new Plan(vehicle.getCurrentCity());
		int numVisited = 0;
		while (!toVisit.isEmpty()) {
			State current = toVisit.remove();

			if (current.isFinal()) {
				actionsCurrent = current.getActions();
				currentPlan = new Plan(vehicle.getCurrentCity(), actionsCurrent);
				costCurrent = currentPlan.totalDistance()*vehicle.costPerKm();
				//System.out.println("Final node encountered!");
				if (costCurrent < optimalCost) {
					optimalCost = costCurrent;
					optimalPlan = currentPlan;
				}

			}

			boolean alreadyVisited = false;
			for(State s: visited) {
				if(s.equals(current)) {
					alreadyVisited = true;
					break;
				}
			}
			if(! alreadyVisited) {
				visited.add(current);
				ArrayList<State> children = current.getReachableStates();
				++numVisited;
				toVisit.addAll(children);
			}

		}
		System.out.println("OPTIMAL PLAN FOUND WITH BFS " + numVisited + " ITERATIONS");
		return optimalPlan;
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

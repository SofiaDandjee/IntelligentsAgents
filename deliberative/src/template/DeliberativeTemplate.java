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

	enum Algorithm { BFS, ASTAR, NAIVE }
	
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
//		List<Task> t = new ArrayList<Task>(tasks);
		TaskSet carried = vehicle.getCurrentTasks();
		TaskSet delivered = TaskSet.noneOf(tasks);//new ArrayList<>();

		State init = new State(null, current, vehicle, carried, tasks);

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
//			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			System.out.println("BFS ALGORITHM");
			plan = computeFinalPlan(plan, BFS(init, vehicle));
			System.out.println("Cost of optimal plan");
			break;
		case NAIVE:
			// ...
			System.out.println("Naive ALGORITHM");
			plan = naivePlan(vehicle, tasks);
			//System.out.println("Cost of optimal plan");
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}

	private State BFS(State initialNode, Vehicle vehicle) {

		//State Q = initialNode;
		Queue<State> toVisit = new LinkedList<State>();
		toVisit.add(initialNode);
		HashSet<State> visited = new HashSet<State>();
		State optimalState = initialNode;
//		double optimalCost = Double.POSITIVE_INFINITY;
		int numVisited = 0;
		while (!toVisit.isEmpty()) {
			State current = toVisit.remove();
			if (current.isFinal()) {
//			if (current.isFinal() && current.getPlan().totalDistance()*vehicle.costPerKm() < optimalCost) {
				System.out.println("A final node encountered!");
				return current;
			}
			if (!visited.contains(current)) {
				visited.add(current);
				List<State> children = current.getReachableStates2();
				toVisit.addAll(children);
				++numVisited;
				System.out.println(numVisited);
			}

		}
		System.out.println("OPTIMAL PLAN FOUND WITH"+ numVisited+ "ITERATIONS");
		return optimalState;
		// Todo: return fail???
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
	public Plan computeFinalPlan(Plan plan, State state){

		if(state == null){
//			print("no plan");
			return null;
		}
		//reorder nodes
		Stack<State> stack = new Stack<State>();
		stack.push(state);
		while(state.getParent() != null){
			state = state.getParent();
			stack.push(state);
		}

		State n1 = stack.pop();
		State n2 = n1;
		while(!stack.isEmpty()){
			n2 = stack.pop();
			//deliver in city_old, pick up in city_old, move to city_new
			addActions(plan, n1, n2);
			n1 = n2;
		}
		//deliver the tasks on the last city.
		TaskSet lastDelivery = n1.getCarried();
		for(Task task : lastDelivery)
			plan.appendDelivery(task);


		return plan;
	}
	public void addActions(Plan plan, State n1, State n2){

		//Deliver tasks
//		TaskSet delivery = TaskSet.intersectComplement(n2.getDelivered(), n1.getDelivered());
		TaskSet delivery = TaskSet.intersectComplement(n1.getCarried(), n2.getCarried());
		for(Task task : delivery)
			plan.appendDelivery(task);

		//Pickup tasks
		TaskSet pickup = TaskSet.intersectComplement(n1.getUnhandled(), n2.getUnhandled());
		for(Task task : pickup)
			plan.appendPickup(task);

		//Move city
		plan.appendMove(n2.getAgentCity());
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

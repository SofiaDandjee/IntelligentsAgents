package template;

//the list of imports
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionPlanner implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private long timeout_plan;
	private boolean recomputed;
	private Planner auctionPlanner;
	private Planner opponentPlanner;
	private Integer numAuctions;
	private Long winningBids;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();
		this.auctionPlanner = new Planner(agent.vehicles());
		this.numAuctions = 0;
		//Suppose the opponent has same vehicles
		this.opponentPlanner = new Planner(agent.vehicles());
		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
		this.winningBids = (long) 0;

		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		/**
		 * This signal informs the agent about the outcome of an auction.
		 * lastWinner is the id of the agent that won the task.
		 * The actual bids of all agents is given as an array lastOffers indexed by agent id.
		 * A null offer indicates that the agent did not participate in the auction.
		 */
		if (winner == agent.id()) {

			System.out.println("You have won task "+ previous.id);
//			currentCity = previous.deliveryCity;
			//update own plan
			win(auctionPlanner, previous);
			winningBids += bids[agent.id()];

		} else {
			//update plan of opponent
			System.out.println("Your opponent has won task "+ previous.id);

			win(opponentPlanner, previous);
		}

	}
	
	@Override
	public Long askPrice(Task task) {
		/**
		 * Asks the agent to offer a price for a task and it is sent for each task that is auctioned.
		 * The agent should return the amount of money it would like to receive for delivering that task.
		 * If the agent wins the auction, it is assigned the task, and it must deliver it in the final plan.
		 * The reward of the task will be set to the agent’s price.
		 * It is possible to return null to reject the task unconditionally.
		 */
		System.out.println("Task " + task.id + " is auctioned.");
		if (vehicle.capacity() < task.weight)
			return null;

		numAuctions+=1;
//		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
//		long distanceSum = distanceTask
//				+ currentCity.distanceUnitsTo(task.pickupCity);
//		double marginalCost = Measures.unitsToKM(distanceSum
//				* vehicle.costPerKm());

		//bidding parameters
		int conservativeAuctions = 5;

		//compute marginal opponent cost
		double marginalOpponentCost = computeMarginalCost(opponentPlanner, task);

		//estimate his bid
		//double opponentBid = 1.1*marginalOpponentCost;

		//compute own marginal cost
		double marginalCost = computeMarginalCost(auctionPlanner, task);

		//lowest bid possible
//		double minimumBid = 0.75*marginalCost;

		double epsilon = 1;

		//bid slightly lower than opponent
		double finalMarginalCost = marginalOpponentCost - epsilon;

		//double finalMarginalCost;

//		if (finalMarginalCost < minimumBid) {
//			finalMarginalCost = minimumBid+epsilon;
//		}

		//take into account probability distributions of tasks


		double ratio;
		// bid less at first rounds
		if (numAuctions <=conservativeAuctions) {
			ratio = (double) numAuctions/conservativeAuctions;
		} else {
			ratio = 1.05;
		}
		//double ratio = 0.5 * numAuctions;

		//if marginal cost or opponent cost is 0, bid very small
		if (marginalCost == 0 || marginalOpponentCost == 0) {
			finalMarginalCost = epsilon;
		}

		double bid = ratio * finalMarginalCost;


		return (long) Math.floor(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		System.out.println("Computing plan.");
//		Plan planVehicle1 = naivePlan(vehicle, tasks);

//		List<Plan> plans = new ArrayList<Plan>();
//		plans.add(planVehicle1);
//		while (plans.size() < vehicles.size())
//			plans.add(Plan.EMPTY);

//		return plans;

		long time_start = System.currentTimeMillis();

//		Planner planner = new Planner(agent.vehicles(), new ArrayList<>(tasks), time_start, timeout_plan);

//		planner.SLS();

		System.out.println("The total-cost is " + auctionPlanner.getBestCost());
		System.out.println("The total-wining score is " + winningBids);
		List<Plan> plans = buildPlan(auctionPlanner.getBestSolution(), vehicles);

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");

		return plans;
	}

	public static List<Plan> buildPlan(Solution s, List<Vehicle> vehicles) {

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

	double computeMarginalCost(Planner plan, Task t) {
//		System.out.println("Computing marginal cost with task "+t.id);
		List<Task> previous = new ArrayList<>(plan.getTasks());
		previous.add(t);
		Planner plannerWithTask = new Planner(plan.vehicles, previous);
		//for (Task t : tasks) {
		//plannerWithTask.addTask(t);
		//}
//		System.out.println(plan.getTasks());
//		System.out.println(plannerWithTask.getTasks());
		plannerWithTask.SLS();

		return plannerWithTask.getBestCost() - plan.getBestCost();
	}

	void win(Planner plan, Task task) {
//		System.out.println("Computing new plan with task "+task.id);
//		List<Task> previous = plan.getTasks();
//		previous.add(task);
//		System.out.println(previous);
//		plan = new Planner(plan.vehicles, previous);
//		System.out.println(plan.getTasks());
		plan.addTask(task);
//		System.out.println(plan.getTasks());
		plan.SLS();
	}
}

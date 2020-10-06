package template;

import java.util.*;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private List<State> states;
	private List<Act> actions;
	private HashMap<State, Act> Vact;

	class Act {
		boolean pickUp;
		double reward;
		private City destination;

		Act(City d, Boolean pu, double r) {
			pickUp = pu;
			reward = r;
			destination = d;
		}

		public void print() {
			if (pickUp) {
				System.out.print("Pick up");
			} else {
				System.out.print("Move");
			}
			System.out.println(" to " + destination);
			System.out.println("With reward ");
			System.out.println(reward);
		}

		public City getDestination() {
			return destination;
		}

		boolean pickUp() {
			return pickUp;
		}

		void setReward(double r) {
			reward = r;
		}

		public double getReward() {
			return reward;
		}
	}

	class State {
		private City location;
		private City destination;
		private Boolean task;

		public List<Act> actions;

		void addAction(Act act) {
			actions.add(act);
		}

		Act getBestAction() {
			double score = Double.NEGATIVE_INFINITY;
			Act best = null;
			for (Act action : actions) {
				if (action.getReward() >= score) {
					best = action;
					score = best.getReward();
				}
			}
			return best;
		}

		public void printState() {
			System.out.println("At " + location );
			if (task) {
				System.out.println("With task to " + destination);
			} else {
				System.out.println("With no task");
			}
			System.out.println("Best action is ");
			getBestAction().print();
		}
		public State(City c, City c1, Boolean b) {
			location = c;
			destination = c1;
			task = b;
			actions = new ArrayList<>();
		}

	}
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		double precisionforV = 1.0;
		int maxiterationsforV = 10000;
		actions = new ArrayList<>();
		states = new ArrayList<>();
		for (City destination : topology) {
			actions.add(new Act(destination, true, 0));
			actions.add(new Act(destination, false, 0));
		}
		// initializing all the possible states
		//Create every possible state
		states = new ArrayList<>();
		for (City origin : topology) {
			for (City destination : topology) {
				if (origin != destination) {
					states.add(new State(origin, destination, true));
				}
			}
			states.add(new State(origin,null, false));
		}
		// Adding possible actions for each state
		for (State state : states) {
			for (Act action: actions) {
				if (state.task && action.pickUp && action.destination == state.destination) {
					state.addAction(action);
				} else if (state.task && !action.pickUp && state.location.hasNeighbor(action.destination)) {
					state.addAction(action);
				} else if (!state.task && !action.pickUp && state.location.hasNeighbor(action.destination)) {
					state.addAction(action);
				}
			}
		}

		HashMap<State, Double> V = new HashMap<State, Double>();
		Vact = new HashMap<State, Act>();
		//Initialize V values
		for (State state : states) {
			V.put(state, 0.0);
		}
		int i = 0;
		boolean nottunedEnough = true;
		System.out.println("Training...");

		while (nottunedEnough) {
			++i;
			double qValue;
			double bestqValue;
			nottunedEnough = false;
			for (State state : states) {
				bestqValue = 0.0;
				for (Act action : state.actions) {
						qValue = 0.0;
						if (action.pickUp && state.task && action.destination == state.destination) {
							qValue += td.reward(state.location, state.destination)/state.location.distanceTo(state.destination)-agent.vehicles().get(0).costPerKm();
						} else {
							qValue -= agent.vehicles().get(0).costPerKm();
						}
						for (State nextState : V.keySet()) {
							if (nextState.location == action.destination ) {
								qValue+= discount*td.probability(nextState.location,nextState.destination)*V.get(nextState);
							}
						}
						action.setReward(qValue);
						if (bestqValue < qValue){
							bestqValue = qValue;
						}
				}
				if(Math.abs(state.getBestAction().getReward()-V.get(state))>precisionforV && i < maxiterationsforV ) {
					nottunedEnough = true;
				}
				V.put(state, state.getBestAction().getReward());
				Vact.put(state, state.getBestAction());
			}
		}
		System.out.println("Took "+i+" iterations to solve");

		for (State state : states) {
			state.printState();
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		System.out.println("Reactive Action.");
		City currentCity = vehicle.getCurrentCity();
		System.out.println("At " + currentCity);
		City nextCityNoTask = null;
		City nextCityTask = null;
		for (State state : states) {
			if (state.location == currentCity && !state.task) {
				//state.printState();
				nextCityNoTask = state.getBestAction().getDestination();
			}
			if (state.location == currentCity && state.task && availableTask != null && state.destination == availableTask.deliveryCity) {
				//state.printState();
				nextCityTask = state.getBestAction().getDestination();
			}
		}

		if (availableTask == null) {
			System.out.println("Task not available.");
			System.out.println("Move to "+ nextCityNoTask.name);
			action = new Move(nextCityNoTask);
		} else {
			System.out.println("Task available to "+ availableTask.deliveryCity);
			if (nextCityTask!= null && availableTask.deliveryCity.name == nextCityTask.name) {
				System.out.println("PickUp to "+ nextCityTask.name);
				action = new Pickup(availableTask);
			} else {
				System.out.println("Move to "+ nextCityNoTask.name);
				action = new Move(nextCityNoTask);
			}
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}

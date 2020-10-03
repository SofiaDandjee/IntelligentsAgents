package template;
import java.util.Random;

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


import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;


public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private double[][] Qtable;
	private double[] Vvalue;
	private int[] Vaction;
	private Topology topology;
	private TaskDistribution td;
	private List<City> cities;
	private int numberOfCities;
	private int numberOfStates;
	private int numberOfActions;
	private HashMap<Integer,Double>[] R;
	private ArrayList<Integer>[] S;
	private double[][][] T;

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


		this.td = td;
		this.topology = topology;

		numberOfCities = topology.size();
		numberOfStates =  numberOfCities * (numberOfCities);
		numberOfActions = numberOfCities+1;

//		this.Qtable = new double[numberOfStates][numberOfStates+1];
		this.Vvalue = new double[numberOfStates];
		this.Vaction = new int[numberOfStates];
		cities = topology.cities();
		for(int v=0; v<Vvalue.length; v++) {
			Vvalue[v] = 1.0;
		}
		initR(td, agent);
		initS(td, agent);
		initT(td);
		fineTuneVvalues();

	}
	private int indexCityFrom(int state) {
		return state%numberOfCities;
	}

	private int indexCityTo(int state) {
		return state/numberOfCities;
	}
	private void initR(TaskDistribution td, Agent agent) {
		R = (HashMap<Integer,Double>[]) new HashMap[numberOfStates];

		for(int s = 0; s<numberOfStates; s++) {
			R[s] = new HashMap<Integer,Double>();

			City from = cities.get(indexCityFrom(s));
			City to = cities.get(indexCityTo(s));


			for(City neighbor : from) {
				R[s].put(neighbor.id, -from.distanceTo(neighbor)*agent.vehicles().get(0).costPerKm()); // id of the city on which to move
			}

			// if to==from, cannot do the 'pick up' action in this state
			if(to!=from) {
				double reward = td.reward(from, to) - from.distanceTo(to)*agent.vehicles().get(0).costPerKm();
				R[s].put(numberOfCities, reward);
			}
		}
	}

	private void initT(TaskDistribution td) {
		T = new double[numberOfStates][numberOfActions][numberOfStates]; // all init to zeros
		for(int s = 0; s<numberOfStates; s++) {
			City to = cities.get(indexCityTo(s));
			for(int action: S[s]) {
				City dest = to;
				if (action!=numberOfCities) dest = cities.get(action);
				for(int c = 0; c<numberOfCities; c++) {
					int newState = c*numberOfCities+dest.id;
					if(c!=dest.id) T[s][action][newState] = td.probability(dest, cities.get(c));
					else T[s][action][newState] = td.probability(dest, null);
				}
			}
		}
	}
	private void initS(TaskDistribution td, Agent agent) {
		S = (ArrayList<Integer>[]) new ArrayList[numberOfStates];

		for(int s = 0; s<numberOfStates; s++) {
			S[s] = new ArrayList<Integer>();

			City from = cities.get(indexCityFrom(s));
			City to = cities.get(indexCityTo(s));

			for(City neighbor : from) {
				S[s].add(neighbor.id); // id of the city on which to move
			}

			// if to==from, cannot do the 'pick up' action in this state
			if(to!=from) {
				S[s].add(numberOfCities); // id of the 'pick up' action
			}
		}
	}

	private void fineTuneVvalues() {
		boolean tunedEnough = true;
		int count = 0;
		while(tunedEnough) {
			count++;
			//System.out.println(count);
			for(int s = 0; s<S.length; s++) {
				double Q, maxQ=Integer.MIN_VALUE;
				int bestAction = 0;
				for(int a: S[s]) {

					Q = R[s].get(a);
					for(int sp = 0; sp<numberOfStates; sp++) { // optimiser
						Q += pPickup*T[s][a][sp]*Vvalue[sp];
					}
					if(Q>maxQ) {
						maxQ = Q;
						bestAction = a;
					}
				}
				if(Math.abs(Vvalue[s]-maxQ)<10) tunedEnough = false;
				Vvalue[s] = maxQ;
				Vaction[s] = bestAction;
			}
		}

		System.out.println("Terminated");
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		City from = vehicle.getCurrentCity();
		City to = null;

		if(availableTask!=null) to = availableTask.deliveryCity;

		int state = from.id*numberOfCities + from.id;
		if (to!=null) state = to.id*numberOfCities + from.id;

		int bestAction = Vaction[state];

		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;

		if(bestAction == numberOfCities) return new Pickup(availableTask);
		else return new Move(cities.get(bestAction));
	}
}

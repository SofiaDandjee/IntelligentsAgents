import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;


import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {		

		private Schedule schedule;

		private static final int GRIDSIZE = 20;
		private static final int NUMINITRABBITS = 10;
		private static final int NUMINITGRASS = 50;
		private static final int GRASSGROWTHRATE = 5;

		private static final int BIRTHTHRESHOLD = 30;
		private static final int AGENT_MIN_LIFESPAN = 0;
		private static final int AGENT_MAX_LIFESPAN = 100;

		private int gridSize = GRIDSIZE;
		private int numInitRabbits = NUMINITRABBITS;
		private int numInitGrass = NUMINITRABBITS;
		private double grassGrowthRate = GRASSGROWTHRATE;
		private int birthThreshold = BIRTHTHRESHOLD;
		private int agentMinLifespan = AGENT_MIN_LIFESPAN;
		private int agentMaxLifespan = AGENT_MAX_LIFESPAN;
	private ArrayList agentList;

	private RabbitsGrassSimulationSpace rgSpace;

	private OpenSequenceGraph amountOfGrassInSpace;

	private OpenSequenceGraph amountOfRabbitsInSpace;

	class grassInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() {
			return (double)rgSpace.getTotalGrass();
		}
	}

	class rabbitsInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() {
			return (double)rgSpace.getTotalRabbits();
		}
	}


		private DisplaySurface displaySurf;

		public static void main(String[] args) {
			
			System.out.println("Rabbit skeleton");

			SimInit init = new SimInit();
			RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
			// Do "not" modify the following lines of parsing arguments
			if (args.length == 0) // by default, you don't use parameter file nor batch mode 
				init.loadModel(model, "", false);
			else
				init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
			
		}

		public void begin() {
			// TODO Auto-generated method stub
			buildModel();
			buildSchedule();
			buildDisplay();

			displaySurf.display();
			amountOfGrassInSpace.display();
			amountOfRabbitsInSpace.display();
		}

		public void buildModel(){
			System.out.println("Running BuildModel");
			rgSpace = new RabbitsGrassSimulationSpace(gridSize);
			rgSpace.spreadGrass(numInitGrass);

			for(int i = 0; i < numInitRabbits; i++){
				addNewAgent();
			}

			for(int i = 0; i < agentList.size(); i++){
				RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent) agentList.get(i);
				cda.report();
			}
		}

		private void addNewAgent(){
			RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(agentMinLifespan, agentMaxLifespan);
			agentList.add(a);
			rgSpace.addAgent(a);
		}

		public void buildSchedule(){
			System.out.println("Running BuildSchedule");
			class RabbitsGrassStep extends BasicAction {
				public void execute() {
					SimUtilities.shuffle(agentList);
					for(int i =0; i < agentList.size(); i++){
						RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent) agentList.get(i);
						cda.step();
					}

					int deadAgents = reapDeadAgents();

					for(int i =0; i < deadAgents; i++){
						addNewAgent();
					}

					displaySurf.updateDisplay();
				}
			}
			schedule.scheduleActionBeginning(0, new RabbitsGrassStep());

			class GrowGrass extends BasicAction {
				@Override
				public void execute() {
					rgSpace.spreadGrass(GRASSGROWTHRATE);
				}
			}
			schedule.scheduleActionAtInterval(10, new GrowGrass());

			class RabbitReproduction extends BasicAction {
				@Override
				public void execute() {
					SimUtilities.shuffle(agentList);
					for(int i =0; i < agentList.size(); i++){
						RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent) agentList.get(i);
						if (agentMaxLifespan - cda.getStepsToLive() > birthThreshold) {
							addNewAgent();

						}

					}
				}
			}

			class RabbitsGrassCountLiving extends BasicAction {
				public void execute(){
					countLivingAgents();
				}
			}

			schedule.scheduleActionAtInterval(10, new RabbitsGrassCountLiving());

			class RabbitsGrassUpdateGrassInSpace extends BasicAction {
				public void execute(){
					amountOfGrassInSpace.step();
				}
			}
			schedule.scheduleActionAtInterval(10, new RabbitsGrassUpdateGrassInSpace());

			class RabbitsGrassUpdateRabbitsInSpace extends BasicAction {
				public void execute(){
					amountOfRabbitsInSpace.step();
				}
			}
			schedule.scheduleActionAtInterval(10, new RabbitsGrassUpdateRabbitsInSpace());

		}

		public void buildDisplay(){
			System.out.println("Running BuildDisplay");
			ColorMap map = new ColorMap();

			for(int i = 1; i<16; i++){
				map.mapColor(i, new Color(0, (int)(i * 8 + 127), 0));
			}
			map.mapColor(0, Color.white);

			Value2DDisplay displayGrass = new Value2DDisplay(rgSpace.getCurrentGrassSpace(), map);

			Object2DDisplay displayAgents = new Object2DDisplay(rgSpace.getCurrentAgentSpace());
			displayAgents.setObjectList(agentList);

			displaySurf.addDisplayableProbeable(displayGrass, "Grass");
			displaySurf.addDisplayableProbeable(displayAgents, "Agents");

			amountOfGrassInSpace.addSequence("Grass In Space", new grassInSpace());
			amountOfRabbitsInSpace.addSequence("Grass In Space", new rabbitsInSpace());

		}

		public String[] getInitParam() {
			// TODO Auto-generated method stub
			// Parameters to be set by users via the Repast UI slider bar
			// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
			String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "AgentMinLifespan", "AgentMaxLifespan"};
			return params;
		}

		public int getBirthThreshold() {
			return birthThreshold;
		}

	private int reapDeadAgents(){
		int count = 0;
		for(int i = (agentList.size() - 1); i >= 0 ; i--){
			RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent) agentList.get(i);
			if(cda.getStepsToLive() < 1){
				rgSpace.removeAgentAt(cda.getX(), cda.getY());
				//maybe not do that
				rgSpace.spreadGrass(cda.getFood());
				agentList.remove(i);
				count++;
			}
		}
		return count;
	}

	private int countLivingAgents(){
		int livingAgents = 0;
		for(int i = 0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent) agentList.get(i);
			if(cda.getStepsToLive() > 0) livingAgents++;
		}
		System.out.println("Number of living agents is: " + livingAgents);

		return livingAgents;
	}

	public double getGrassGrowthRate() {
		return grassGrowthRate;
	}

	public int getGridSize() {
		return gridSize;
	}

	public int getNumInitGrass() {
		return numInitGrass;
	}

	public int getNumInitRabbits() {
		return numInitRabbits;
	}

	public void setBirthThreshold(int birthThreshold) {
		this.birthThreshold = birthThreshold;
	}

	public void setGrassGrowthRate(double grassGrowthRate) {
		this.grassGrowthRate = grassGrowthRate;
	}

	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

	public void setNumInitGrass(int numInitGrass) {
		this.numInitGrass = numInitGrass;
	}

	public void setNumInitRabbits(int numInitRabbits) {
		this.numInitRabbits = numInitRabbits;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}


	public String getName() {
			// TODO Auto-generated method stub
			return "Rabbits Grass Simulation";
		}

		public Schedule getSchedule() {
			return schedule;
		}

		public void setup() {

			System.out.println("Running setup");
			rgSpace = null;
			agentList = new ArrayList();
			schedule = new Schedule(1);

			if (displaySurf != null){
				displaySurf.dispose();
			}
			displaySurf = null;

			if (amountOfGrassInSpace != null){
				amountOfGrassInSpace.dispose();
			}
			amountOfGrassInSpace = null;

			if (amountOfRabbitsInSpace != null){
				amountOfRabbitsInSpace.dispose();
			}
			amountOfRabbitsInSpace = null;

			displaySurf = new DisplaySurface(this, "Rabbit Grass Model");
			amountOfGrassInSpace = new OpenSequenceGraph("Amount Of Grass In Space",this);
			amountOfRabbitsInSpace = new OpenSequenceGraph("Amount Of Rabbits In Space",this);


			registerDisplaySurface("Rabbit Grass Model", displaySurf);
			this.registerMediaProducer("Plot", amountOfGrassInSpace);
			this.registerMediaProducer("Plot", amountOfRabbitsInSpace);
		}
	public int getAgentMaxLifespan() {
		return agentMaxLifespan;
	}

	public int getAgentMinLifespan() {
		return agentMinLifespan;
	}

	public void setAgentMaxLifespan(int i) {
		agentMaxLifespan = i;
	}

	public void setAgentMinLifespan(int i) {
		agentMinLifespan = i;
	}


}

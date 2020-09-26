import java.awt.Color;

import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	private int x;
	private int y;
	private int vX;
	private int vY;
	private int food;
	private int stepsToLive;
	private static int IDNumber = 0;
	private int ID;
	private RabbitsGrassSimulationSpace rgSpace;

	public RabbitsGrassSimulationAgent(int minLifespan, int maxLifespan){
		x = -1;
		y = -1;
		food = 0;
		setVxVy();
		stepsToLive = (int)((Math.random() * (maxLifespan - minLifespan)) + minLifespan);
		IDNumber++;
		ID = IDNumber;
	}

	private void setVxVy(){
		vX = 0;
		vY = 0;
		while((vX == 0) && ( vY == 0)){
			vX = (int)Math.floor(Math.random() * 3) - 1;
			vY = (int)Math.floor(Math.random() * 3) - 1;
		}
	}

	public void setXY(int newX, int newY){
		x = newX;
		y = newY;
	}

	public void setCarryDropSpace(RabbitsGrassSimulationSpace cds){
		rgSpace = cds;
	}

	public String getID(){
		return "A-" + ID;
	}

	public int getFood(){
		return food;
	}

	public int getStepsToLive(){
		return stepsToLive;
	}

	public void report() {
		System.out.println(getID() +
				" at " +
				x + ", " + y +
				" has " +
				getFood() + " grass" +
				" and " +
				getStepsToLive() + " steps to live.");
	}

	public void draw(SimGraphics arg0) {
		if(stepsToLive > 10)
			arg0.drawFastRoundRect(Color.blue);
		else
		arg0.drawFastRoundRect(Color.red);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void step(){
		int newX = x + vX;
		int newY = y + vY;

		Object2DGrid grid = rgSpace.getCurrentAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		if(tryMove(newX, newY)) {
			food += rgSpace.takeGrassAt(x, y);
		}
		else {
			setVxVy();
			RabbitsGrassSimulationAgent cda = rgSpace.getAgentAt(newX, newY);
			if (cda!= null){
				if(food > 0){
					cda.loseGrass(1);
					food--;
				}
			}
		}
		stepsToLive--;
	}

	private boolean tryMove(int newX, int newY){
		return rgSpace.moveAgentAt(x, y, newX, newY);
	}

	public void receiveGrass(int amount){
		food += amount;
	}
	public void loseGrass(int amount){
		food -= amount;
	}

}

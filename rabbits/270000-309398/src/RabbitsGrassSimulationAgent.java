import java.awt.Color;
import java.io.IOException;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.Graphics;

import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	//Position
	private int x;
	private int y;

	//Speed
	private int vX;
	private int vY;

	private int energy;

	private static int IDNumber = 0;
	private int ID;
	private RabbitsGrassSimulationSpace rgSpace;
	private BufferedImage img;
	private Graphics g;

	/**
	 * Class constructor
	 * @param minEnergy, the minimum energy at birth of the rabbit
	 */
	public RabbitsGrassSimulationAgent(int minEnergy){
		try {
			img = ImageIO.read(new File("img/rabbit.png"));
			//System.out.println("image read succesfull");
		} catch (IOException e) {
			System.out.println("image read not succesfull");

		}
		x = -1;
		y = -1;
		energy = minEnergy;
		setVxVy();
		IDNumber++;
		ID = IDNumber;
	}

	/**
	 * Sets the rabbit speed direction, either north, west, south or east
	 */
	private void setVxVy(){
		vX = 0;
		vY = 0;
		int v = 0;
		//The rabbit either moves in the x direction or the y direction
		v = (int)Math.ceil(Math.random() * 4);

		//We randomly chose a direction
		if (v == 1) {
			vX = 1;
		} else if (v == 2){
			vX = -1;
		} else if (v== 3) {
			vY = 1;
		} else if (v== 4) {
			vY = -1;
		}

	}

	/**
	 * Setter for the rabbit position
	 * @param newX, the new position
	 * @param newY, the new position
	 */
	public void setXY(int newX, int newY){
		x = newX;
		y = newY;
	}

	/**
	 * Sets the space Object of the rabbit
	 * @param cds, the simulation space
	 */
	public void setCarryDropSpace(RabbitsGrassSimulationSpace cds){
		rgSpace = cds;
	}

	/**
	 * Getter for the ID of the rabbit
	 * @return the ID, a String
	 */
	public String getID(){
		return "A-" + ID;
	}

	/**
	 * Getter for the energy
	 * @return energy
	 */
	public int getEnergy(){
		return energy;
	}

	/**
	 * Prints the ID, position and energy of the rabbit
	 */
	public void report() {
		System.out.println(getID() +
				" at " +
				x + ", " + y +
				" has " +
				getEnergy() + " energy.");
	}

	/**
	 * Draws the rabbit either with a buffered image if found, either with a color
	 * @param arg0, the SimGraphics that draws the rabbit
	 */
	public void draw(SimGraphics arg0) {
		if (img != null) {
			arg0.drawImageToFit(img);
		}
		else {
			if(energy > 4)
				arg0.drawFastRoundRect(Color.blue);
			else
				arg0.drawFastRoundRect(Color.red);
		}
	}

	/**
	 * Getter for the x position
	 * @return x
	 */
	public int getX() {
		return x;
	}

	/**
	 * Getter for the y position
	 * @return y
	 */
	public int getY() {
		return y;
	}

	/**
	 * Performs a simulation step
	 * Moves if he can, reproduces if he can and loses energy when he moves
	 * @param energyGain, the energy per grass a rabbit gains
	 * @param lossReproduction, the energy lost when it reproduces
	 * @param birthThreshold, the minimum energy with which he can reproduce
	 * @return true, if he has reproduced, false otherwise
	 */
	public boolean step(int energyGain, int lossReproduction, int birthThreshold) {
		setVxVy();
		int newX = x + vX;
		int newY = y + vY;
		boolean retVal = false;
		Object2DGrid grid = rgSpace.getCurrentAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		if (tryMove(newX, newY)) {
			receiveEnergy(energyGain * rgSpace.takeGrassAt(x, y));
			energy--;
		} else {
			retVal = reproduce(newX, newY, lossReproduction, birthThreshold);
		}

		return retVal;

	}

	/**
	 * Indicates if the rabbit has reproduced with the rabbit he collided with (if both have eneough energy)
	 * @param newX, the x target position eventually containing a rabbit
	 * @param newY, the y target position eventually containing a rabbit
	 * @param lossReproduction, the energy lost when it reproduces
	 * @param birthThreshold, the minimum energy with which he can reproduce
	 * @return true if he has reproduced, false otherwise
	 */
	private boolean reproduce (int newX, int newY, int lossReproduction, int birthThreshold) {
		RabbitsGrassSimulationAgent cda = rgSpace.getAgentAt(newX, newY);
		if (cda!= null){
			if(energy > birthThreshold && cda.getEnergy() > birthThreshold){
				//During a collision, both rabbits lose grass
				cda.loseEnergy(lossReproduction);
				energy -= lossReproduction;
				return true;
			}
		}
		return false;
	}

	/**
	 * Indicates if the rabbit was able to move
	 * @param newX, the target x position
	 * @param newY, the target y position
	 * @return true if he has moved, false otherwise
	 */
	private boolean tryMove(int newX, int newY){
		return rgSpace.moveAgentAt(x, y, newX, newY);
	}

	/**
	 * Increases the rabbit's energy
	 * @param amount, the amount of energy received
	 */
	public void receiveEnergy(int amount){
		energy += amount;
	}

	/**
	 * Decreases the rabbit's energy
	 * @param amount, the amount of energy lost
	 */
	public void loseEnergy(int amount){
		energy -= amount;
	}

}

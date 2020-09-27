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
	private int x;
	private int y;
	private int vX;
	private int vY;
	private int energy;

	private static int IDNumber = 0;
	private int ID;
	private RabbitsGrassSimulationSpace rgSpace;
	private BufferedImage img;
	private Graphics g;
//	private int ENERGY_GAIN;

	public RabbitsGrassSimulationAgent(int minEnergy){
		try {
			img = ImageIO.read(new File("img/rabbit.png"));
			System.out.println("image read succesfull");
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

	private void setVxVy(){
		vX = 0;
		vY = 0;
		int v = 0;
		//The rabbit either moves in the x direction or the y direction, the rabbit can stay in the same cell as well
		//We randomly chose a speed (-1 or 1)

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

	public int getEnergy(){
		return energy;
	}

	public void report() {
		System.out.println(getID() +
				" at " +
				x + ", " + y +
				" has " +
				getEnergy() + " energy.");
	}

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

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void step(int energyGain){
		setVxVy();
		int newX = x + vX;
		int newY = y + vY;

		Object2DGrid grid = rgSpace.getCurrentAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		if(tryMove(newX, newY)) {
//			energy += energyGain*rgSpace.takeGrassAt(x, y);
			receiveEnergy(energyGain*rgSpace.takeGrassAt(x, y));
		}
//		else { // else do nothing
			//setVxVy();
			/**RabbitsGrassSimulationAgent cda = rgSpace.getAgentAt(newX, newY);
			if (cda!= null){
				if(energy > 0){
					//During a collision, both rabbits lose grass
					cda.loseEnergy(1);
					energy--;
				}
			}**/
//		}
		energy--;
	}

	private boolean tryMove(int newX, int newY){
		return rgSpace.moveAgentAt(x, y, newX, newY);
	}

	public void receiveEnergy(int amount){
		energy += amount;
	}
	public void loseEnergy(int amount){
		energy -= amount;
	}

}

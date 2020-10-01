/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */
import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {

    private Object2DGrid grassSpace;
    private Object2DGrid agentSpace;

    /**
     * Class constructor
     * @param size, size of the grid space
     */
    public RabbitsGrassSimulationSpace (int size) {
        grassSpace = new Object2DGrid (size,size);
        agentSpace = new Object2DGrid(size,size);

        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                grassSpace.putObjectAt(i,j,new Integer(0));
            }
        }
    }

    /**
     * Spreads grass randomly on the space, either where is already some grass or no grass
     * @param grass, number of grass to be spread
     */
    public void spreadGrass(int grass){

        // Randomly place grass in grassSpace
        for(int i = 0; i < grass; i++){

            // Choose coordinates
            int x = (int)(Math.random()*(grassSpace.getSizeX()));
            int y = (int)(Math.random()*(grassSpace.getSizeY()));

            // Get the value of the object at those coordinates
            int I;
            if(grassSpace.getObjectAt(x,y)!= null){
                I = ((Integer)grassSpace.getObjectAt(x,y)).intValue();
            }
            else{
                I = 0;
            }

            grassSpace.putObjectAt(x,y,new Integer(I + 1));
        }
    }

    /**
     * Returns the value of the grass recovered in a cell
     * @param x, the x position of the cell
     * @param y, the y position of the cell
     * @return the energy of the grass
     */
    public int getGrassAt(int x, int y){
        int i;
        if(grassSpace.getObjectAt(x,y)!= null){
            i = ((Integer)grassSpace.getObjectAt(x,y)).intValue();
        }
        else{
            i = 0;
        }
        return i;
    }

    /**
     * Getter for the grass space
     * @return the grass space
     */
    public Object2DGrid getCurrentGrassSpace(){
        return grassSpace;
    }

    /**
     * Getter for the agentSpace
     * @return the agent space
     */
    public Object2DGrid getCurrentAgentSpace(){
        return agentSpace;
    }

    /**
     * Indicates if a cell is occupied
     * @param x, the x position of the cell
     * @param y, the y position of the cell
     * @return true if the cell is occupied, false if not
     */
    public boolean isCellOccupied(int x, int y){
        boolean retVal = false;
        if(agentSpace.getObjectAt(x, y)!=null) retVal = true;
        return retVal;
    }

    /**
     * Adds an agent at a random available cell
     * @param agent, the agent to be added
     * @return true if the operation was successfull, false if not
     */
    public boolean addAgent(RabbitsGrassSimulationAgent agent){
        boolean retVal = false;
        int count = 0;
        int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();

        while((retVal==false) && (count < countLimit)){
            int x = (int)(Math.random()*(agentSpace.getSizeX()));
            int y = (int)(Math.random()*(agentSpace.getSizeY()));
            if(isCellOccupied(x,y) == false){
                agentSpace.putObjectAt(x,y,agent);
                agent.setXY(x,y);
                agent.setCarryDropSpace(this);
                retVal = true;
            }
            count++;
        }

        return retVal;
    }

    /**
     * Removes an agent Object from a given cell
     * @param x, the x position of the cell
     * @param y, the y position of the cell
     */
    public void removeAgentAt(int x, int y){
        agentSpace.putObjectAt(x, y, null);
    }

    /**
     * Empties grass from a cell
     * @param x, x position of the cell
     * @param y, y position of the cell
     * @return the value of the grass retrieved
     */
    public int takeGrassAt(int x, int y){
        int food = getGrassAt(x, y);
        grassSpace.putObjectAt(x, y, new Integer(0));
        return food;
    }

    /**
     * Moves an agent Object from a position to another, if the target position is unoccupied
     * @param x, actual x position
     * @param y, actual y position
     * @param newX, target x position
     * @param newY, target y position
     * @return true if the agent was able to move, false otherwise
     */
    public boolean moveAgentAt(int x, int y, int newX, int newY){
        boolean retVal = false;
        if(!isCellOccupied(newX, newY)){
            RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent) agentSpace.getObjectAt(x, y);
            removeAgentAt(x,y);
            rga.setXY(newX, newY);
            agentSpace.putObjectAt(newX, newY, rga);
            retVal = true;
        }
        return retVal;
    }

    /**
     * Returns the agent Object situated in a cell
     * @param x, the x position of the cell
     * @param y, the y position of the cell
     * @return the rabbit found, null if none is found
     */
    public RabbitsGrassSimulationAgent getAgentAt(int x, int y){
        RabbitsGrassSimulationAgent retVal = null;
        if(agentSpace.getObjectAt(x, y) != null){
            retVal = (RabbitsGrassSimulationAgent) agentSpace.getObjectAt(x,y);
        }
        return retVal;
    }

    /**
     * Returns total number of grass at a given simulation step
     * @return total number of grass
     */
    public int getTotalGrass(){
        int totalGrass = 0;
        for(int i = 0; i < agentSpace.getSizeX(); i++){
            for(int j = 0; j < agentSpace.getSizeY(); j++){
                totalGrass += getGrassAt(i,j);
            }
        }
        return totalGrass;
    }

    /**
     * Returns total number of rabbits at a given simulation step
     * @return totalRabbits, total number of rabbits
     */
    public int getTotalRabbits(){
        int totalRabbits = 0;
        for(int i = 0; i < agentSpace.getSizeX(); i++){
            for(int j = 0; j < agentSpace.getSizeY(); j++){
                if (isCellOccupied(i,j)) {
                    ++totalRabbits;
                }
            }
        }

        return totalRabbits;
    }
}

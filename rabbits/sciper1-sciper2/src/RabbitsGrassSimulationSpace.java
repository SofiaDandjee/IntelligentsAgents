/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */
import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {

    private Object2DGrid grassSpace;
    private Object2DGrid agentSpace;

    public RabbitsGrassSimulationSpace (int size) {
        grassSpace = new Object2DGrid (size,size);
        agentSpace = new Object2DGrid(size,size);

        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                grassSpace.putObjectAt(i,j,new Integer(0));
            }
        }
    }

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
            // Put the grass next to the one already there

            //TO DO: torus
            grassSpace.putObjectAt(x,y,new Integer(I + 1));
        }
    }

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

    public Object2DGrid getCurrentGrassSpace(){
        return grassSpace;
    }

    public Object2DGrid getCurrentAgentSpace(){
        return agentSpace;
    }

    public boolean isCellOccupied(int x, int y){
        boolean retVal = false;
        if(agentSpace.getObjectAt(x, y)!=null) retVal = true;
        return retVal;
    }

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

    public void removeAgentAt(int x, int y){
        agentSpace.putObjectAt(x, y, null);
    }

    public int takeGrassAt(int x, int y){
        int food = getGrassAt(x, y);
        grassSpace.putObjectAt(x, y, new Integer(0));
        return food;
    }

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

    public RabbitsGrassSimulationAgent getAgentAt(int x, int y){
        RabbitsGrassSimulationAgent retVal = null;
        if(agentSpace.getObjectAt(x, y) != null){
            retVal = (RabbitsGrassSimulationAgent) agentSpace.getObjectAt(x,y);
        }
        return retVal;
    }

    public int getTotalGrass(){
        int totalMoney = 0;
        for(int i = 0; i < agentSpace.getSizeX(); i++){
            for(int j = 0; j < agentSpace.getSizeY(); j++){
                totalMoney += getGrassAt(i,j);
            }
        }
        return totalMoney;
    }

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

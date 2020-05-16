package eu.su.mas.dedaleEtu.mas.agents.dummies;

import java.util.ArrayList;
import java.util.HashMap;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.IExploBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.IPingBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.strategy.BossStrategy;
import eu.su.mas.dedaleEtu.mas.strategy.ExploSoloStrategy;
import jade.core.AID;

public abstract class AbstractExploAgent extends AbstractDedaleAgent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected MapRepresentation myMap;
	protected IExploBehaviour explo;
	protected IPingBehaviour ping;
	protected int timeCounter;
	//gestion des collisions:
	protected String collision;
	protected String actual;
	protected boolean obstacle;
	protected HashMap<String,Integer> wumpus;
	protected ArrayList<String> stench;
	protected String coalitionName = "";
	protected ArrayList<AID> coalitionMembers = new ArrayList<>();
	protected ArrayList<String> coalitionDone = new ArrayList<>();
	protected String coalitionSpawnPoint = "";
	protected String boss = "";
	protected String coalitionTarget = "";
	protected ExploSoloStrategy strategie_explo_solo;
	protected BossStrategy bossStrat;
	
	public BossStrategy getBossStrat() {
		return this.bossStrat;
	}
	
	public ExploSoloStrategy getExploSoloStrategy() {
		return strategie_explo_solo;
	}
	
	private String supposedPosition;
	
	
	public String getSupposedPos() {
		return this.supposedPosition;
	}
	
	public void setSupposedPos(String pos) {
		this.supposedPosition = pos;
	}
	
	public void wumpusSeen(String pos) {
		System.err.println("WUMPUS FOUND");
		this.wumpus.put(pos, this.timeCounter);
	}
	
	public void wumpusSeen(String id, int time) {
		System.err.println("WUMPUS FOUND");
		this.wumpus.put(id, time);
	}
	
	public void removeWumpus(String id) {
		if(this.wumpus.containsKey(id)) {
			this.wumpus.remove(id);
		}
	}
	
	public boolean wumpusSeen() {
		return this.wumpus.size()>0;
	}
	
	public String lastWumpusSeen() {
		String lastPos = "";
		for(String pos : this.wumpus.keySet()) {
			if(lastPos.equalsIgnoreCase("") || this.wumpus.get(pos)>this.wumpus.get(lastPos)) {
				lastPos = pos;
			}
		}
		return lastPos;
	}
	
	public int wumpusSeenAt(String wumpus) {
		if(this.wumpus.containsKey(wumpus)) {
			return this.wumpus.get(wumpus);
		}
		return Integer.MAX_VALUE;
	}
	
	public void putStench(String id) {
		if(!this.stench.contains(id)) {
			this.stench.add(id);
		}
	}
	
	public void noStench(String id) {//retire un noeud si il n'a pas/plus d'odeur
		if(this.stench.contains(id)) {
			this.stench.remove(id);
		}
	}
	
	public boolean isStench(String pos) {
		return this.stench.contains(pos);
	}
	
	public ArrayList<String> getStench(){
		return this.stench;
	}
	
	public void setCollision(String actual, String obstacle) {
		System.out.println(this.getName()+": COLLISION ADDES IN AGENT");
		this.collision = obstacle;
		this.obstacle = true;
		this.actual = actual;
	}
	
	public void resetCollision() {
		this.obstacle = false;
	}
	
	public boolean isCollision() {
		return this.obstacle;
	}
	
	public String getCollision() {
		return this.collision;
	}
	
	public String getActual() {
		return this.actual;
	}
	
	public int getTime() {
		return this.timeCounter;
	}
	
	public void incrementTime() {
		this.timeCounter+=1;
	}
	
	public MapRepresentation getMap() {
		return myMap;
	}
	
	public IPingBehaviour getPing() {
		return ping;
	}
	
	public IExploBehaviour getExplo() {
		return explo;
	}
	

}

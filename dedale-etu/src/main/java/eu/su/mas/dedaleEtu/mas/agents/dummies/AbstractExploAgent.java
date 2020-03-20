package eu.su.mas.dedaleEtu.mas.agents.dummies;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploSoloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PingBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

public abstract class AbstractExploAgent extends AbstractDedaleAgent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected MapRepresentation myMap;
	protected ExploSoloBehaviour explo;
	protected PingBehaviour ping;
	protected int timeCounter;
	//gestion des collisions:
	protected String collision;
	protected String actual;
	protected boolean obstacle;
	
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
	
	public PingBehaviour getPing() {
		return ping;
	}
	
	public ExploSoloBehaviour getExplo() {
		return explo;
	}
	

}

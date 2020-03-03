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
	
	public MapRepresentation getMap() {
		return myMap;
	}
	
	public PingBehaviour getPing() {
		return ping;
	}
	
	public ExploSoloBehaviour getExplo() {
		return this.explo;
	}
	

}

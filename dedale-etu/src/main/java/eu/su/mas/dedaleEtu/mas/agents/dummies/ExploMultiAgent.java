package eu.su.mas.dedaleEtu.mas.agents.dummies;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploSoloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ReceiveMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SendMapBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

/**
 * <pre>
 * ExploreSolo agent. 
 * It explore the map using a DFS algorithm.
 * It stops when all nodes have been visited.
 *  </pre>
 *  
 * @author hc
 *
 */

public class ExploMultiAgent extends AbstractExploAgent {

	private static final long serialVersionUID = -6431752665590433727L;

	private SendMapBehaviour sendMap;
	private ReceiveMapBehaviour receiveMap;
	
	
	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();
		
		this.timeCounter = 0;
		List<Behaviour> lb=new ArrayList<Behaviour>();
		
		/************************************************
		 * 
		 * ADD the behaviours of the Dummy Moving Agent
		 * 
		 ************************************************/
		this.explo = new ExploSoloBehaviour(this);
		this.ping = new PingBehaviour(this);
		this.sendMap = new SendMapBehaviour(this);
		this.receiveMap = new ReceiveMapBehaviour(this);
		
		FSMBehaviour fsm = new FSMBehaviour(this);
		final String EXPLO = "Explo";
		final String PING = "Ping";
		final String SEND = "Send";
		final String REC = "Receive";
		//on défini les états :
		fsm.registerFirstState(this.ping,PING);
		fsm.registerState(this.explo,EXPLO);
		fsm.registerState(this.sendMap,SEND);
		fsm.registerState(this.receiveMap,REC);
		//on défini les transitions :
		fsm.registerTransition(EXPLO, PING,0);
		fsm.registerTransition(SEND, REC,0);
		fsm.registerTransition(REC, EXPLO,0);
		fsm.registerTransition(PING, SEND, 1);
		fsm.registerTransition(PING, EXPLO, 0);
		lb.add(fsm);
		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */
		
		
		addBehaviour(new startMyBehaviours(this,lb));
		
		System.out.println("the  agent "+this.getLocalName()+ " is started");

	}
	
	
	
}

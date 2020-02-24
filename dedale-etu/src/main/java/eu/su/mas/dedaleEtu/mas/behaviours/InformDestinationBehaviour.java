package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.graphstream.graph.Graph;

import java.time.*;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreMultiAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class InformDestinationBehaviour  extends SimpleBehaviour{

	private static final long serialVersionUID = -2058134622078521998L;

	private String receiver;
	private ExploreSoloAgent agent;
	
	public InformDestinationBehaviour(ExploreSoloAgent myagent,String receiver) {
		this.agent= myagent;
		this.receiver = receiver;
	}
	
	@Override
	public void action() {
		ACLMessage message = new ACLMessage(ACLMessage.INFORM);
		message.addReceiver(new AID(this.receiver,AID.ISLOCALNAME));
		message.setSender(this.agent.getAID());
		message.setContent(this.agent.getCurrentPosition()+","+this.agent.explo.nextNode);//envoie la case où il est, où il veut aller
		System.out.println("Send message : "+this.agent.getCurrentPosition()+","+this.agent.explo.nextNode);
		this.agent.sendMessage(message);
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}
}

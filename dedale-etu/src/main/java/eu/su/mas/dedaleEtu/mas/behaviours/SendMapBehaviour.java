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

public class SendMapBehaviour  extends SimpleBehaviour{

	private static final long serialVersionUID = -2058134622078521998L;

	private String receiver;
	private ExploreSoloAgent agent;
	
	public SendMapBehaviour(ExploreSoloAgent myagent,String receiver) {
		this.agent= myagent;
		this.receiver = receiver;
	}
	
	/*
	@Override
	public void action() {
		if(this.agent.myMap!=null) {
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			message.addReceiver(new AID(this.receiver,AID.ISLOCALNAME));
			message.setSender(this.agent.getAID());
			JSONObject jsMap = new JSONObject();
			jsMap.putAll(this.agent.myMap.getGraph());//on sérialise en JSON le graphe
			String jsonToSend = myMap.toJSONString();
			try {
				message.setContent(jsonToSend);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.agent.send(message);//envoi de la map
		}
	}*/
	
	@Override
	public void action() {
		if(this.agent.getMap()!=null) {
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			message.addReceiver(new AID(this.receiver,AID.ISLOCALNAME));
			message.setSender(this.agent.getAID());
			message.setContent(this.agent.getMap().serialize());//we send serialised graph
			this.agent.sendMessage(message);
		}
	}

	
	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	} 
}

package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.graphstream.graph.Graph;

import java.time.*;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractExploAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class PingBehaviour  extends OneShotBehaviour{

	private static final long serialVersionUID = -2058134622078521998L;

	private AbstractExploAgent agent;
	private ArrayList<String[]> sonarInfos;
	private ArrayList<AID> targetList;

	
	public PingBehaviour(AbstractExploAgent myagent) {
		this.agent= myagent;

		this.sonarInfos = new ArrayList<>();
		this.targetList = new ArrayList<>();
	}
	
	public ArrayList<AID> getTargetList() {
		return this.targetList;
	}

	@Override
	public void action() {
		this.agent.incrementTime();//on incrémente le pas de temps de l'agent.
		//System.out.println(this.agent.getName()+": PING");
		sonarInfos = new ArrayList<>();
		targetList = new ArrayList<>();
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		//récupérer liste des agents
		AMSAgentDescription[] agents = null;
		try {
			SearchConstraints c = new SearchConstraints();
			c.setMaxResults(new Long(-1));
			agents = AMSService.search(this.agent,  new AMSAgentDescription(),c);
		}catch(Exception e) {
			System.err.println(e);
		}
		//System.out.println("Explo : "+this.agent.getExplo());
		if(agents!=null && this.agent.getExplo()!=null && this.agent.getExplo().getNextNode()!=null) {
			for(int i = 0; i<agents.length;i++) {
				message.addReceiver(agents[i].getName());
				//System.out.println(agents[i].getName());
			}
			message.setSender(this.agent.getAID());
			message.setContent(this.agent.getCurrentPosition()+","+this.agent.getExplo().getNextNode());//envoie la case où il est, où il veut aller
			//System.out.println("Send message : "+this.agent.getCurrentPosition()+","+this.agent.getExplo().getNextNode());
			this.agent.sendMessage(message);
		}
		//fin envoi des ping
		//attente d'un ping
		try {
			this.agent.doWait(100);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//réception des pings : 
		MessageTemplate mt = MessageTemplate.MatchPerformative( ACLMessage.PROPOSE );
		ACLMessage msg = this.agent.receive(mt);
		while(msg!=null) {
			if(msg.getSender().getName()!=this.agent.getAID().getName()) {//si c'est un autre agent qui a envoyé le ping
				//traitement du message : 
				if(!this.targetList.contains(msg.getSender())) {
					this.targetList.add(msg.getSender());
				}
				String toSplit = msg.getSender().getName()+","+msg.getContent();
				String[] info = toSplit.split(",");
				this.sonarInfos.add(info);
				if(info[1].equalsIgnoreCase(this.agent.getExplo().getNextNode()) && info[2].equalsIgnoreCase(this.agent.getCurrentPosition())
						|| info[2].equalsIgnoreCase(this.agent.getExplo().getNextNode())) {//si croisement ou même destination
					//collision
					System.out.println(this.agent.getName()+" : COLLISION");
					//ADDED: tenir compte de l'obstacle
					this.agent.setCollision(this.agent.getCurrentPosition(),this.agent.getExplo().getNextNode());
					
				}
				System.out.println(this.agent.getName()+": Received : "+toSplit+" to "+this.agent.getAID().getName());
			}
			//suivant :
			msg = this.agent.receive(mt);
		}
	}
	
	public int onEnd() {
		if(this.targetList.size()>0) { //si on a détecté des agents autour
			//System.out.println(this.agent.getName()+": PING END -> 1");
			return 1;
		}
		//System.out.println(this.agent.getName()+": PING END -> 0");
		return 0;
	}

}

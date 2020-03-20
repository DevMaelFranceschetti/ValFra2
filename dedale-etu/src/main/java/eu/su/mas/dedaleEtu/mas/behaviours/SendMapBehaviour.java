package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.graphstream.graph.Graph;

import java.time.*;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractExploAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SendMapBehaviour  extends OneShotBehaviour{

	private static final long serialVersionUID = -2058134622078521998L;
	private final int refresh_time = 10;// durée minimale entre deux envois de la carte à un agent (durée incrémentée de 1 à chaque ping)

	private AbstractExploAgent agent;
	private HashMap<AID,Integer> recentlySend;
	
	public SendMapBehaviour(AbstractExploAgent myagent) {
		this.agent= myagent;
		this.recentlySend = new HashMap<>();
	}
	
	@Override
	public void action() {
		if(this.agent.getExplo().getMap()!=null) {
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			if(this.agent.getPing()!=null) {
				int nbReceivers = 0;
				for(AID agent : this.agent.getPing().getTargetList()) {
					if( !this.recentlySend.containsKey(agent) || this.agent.getTime()-this.recentlySend.get(agent)>refresh_time) {//si on a pas envoyé à cet agent depuis plus de [refresh_time] unités de temps
						message.addReceiver(agent);
						nbReceivers+=1;
						this.recentlySend.put(agent, this.agent.getTime());//on mémorise le fait qu'on envoie la carte à ces agents maintenant
						//System.out.print("Receiver : "+agent);
					}
					else {
						System.out.println(this.agent.getName()+": Already send recently to "+agent.getName());
					}
				}
				if(nbReceivers >0) {//si on a déjà envoyé récemment à tous les agents pingés, on ne renvoie pas.
					message.setSender(this.agent.getAID());
					message.setContent(this.agent.getExplo().getMap().serialize());//we send serialised graph
					this.agent.sendMessage(message);
					//System.out.println(this.agent.getName()+": Map send : "+message);
				}
			}
		}
	}
	
	public int onEnd() {
		//System.out.println(this.agent.getName()+": SEND END -> 0");
		return 0;
	}

}

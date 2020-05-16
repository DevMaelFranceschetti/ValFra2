package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import org.graphstream.graph.Graph;
import java.time.*;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractExploAgent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ReceiveMapBehaviour  extends OneShotBehaviour{

	private static final long serialVersionUID = -2058134622078521998L;

	private AbstractExploAgent agent;

	public ReceiveMapBehaviour(AbstractExploAgent myagent) {
		this.agent= myagent;
	}

	@Override
	public void action() {
		MessageTemplate mt = MessageTemplate.MatchPerformative( ACLMessage.INFORM );
		ACLMessage message = this.agent.receive(mt);
		if(message!=null) {
			String data = message.getContent();
			this.agent.getExplo().getMap().unserialize(data);
			System.err.println(this.agent.getName()+": msg received : "+data);
		}
	}
	
	public int onEnd() {
		return 0;
	}

}

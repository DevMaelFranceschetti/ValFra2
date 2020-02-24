package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import org.graphstream.graph.Graph;
import java.time.*;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreMultiAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent2;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ReceiveMapBehaviour  extends SimpleBehaviour{

	private static final long serialVersionUID = -2058134622078521998L;

	private ExploreSoloAgent2 agent;

	public ReceiveMapBehaviour(ExploreSoloAgent2 myagent) {
		this.agent= myagent;
	}

	@Override
	public void action() {
		ACLMessage message = this.agent.receive();
		if(message!=null) {
			String data = message.getContent();
			this.agent.getMap().unserialize(data);
		}
	}

	@Override
	public boolean done() {
		return false;
	}
}

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
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SendMapBehaviour  extends OneShotBehaviour{

	private static final long serialVersionUID = -2058134622078521998L;

	private AbstractExploAgent agent;
	
	public SendMapBehaviour(AbstractExploAgent myagent) {
		this.agent= myagent;
	}
	
	@Override
	public void action() {
		if(this.agent.getExplo().getMap()!=null) {
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			/*start
			AMSAgentDescription[] agents = null;
			try {
				SearchConstraints c = new SearchConstraints();
				c.setMaxResults(new Long(-1));
				agents = AMSService.search(this.agent,  new AMSAgentDescription(),c);
			}catch(Exception e) {
				System.err.println(e);
			}
			for(int i = 0; i<agents.length;i++) {
				message.addReceiver(agents[i].getName());
				//System.out.println(agents[i].getName());
			}
			end*/
			if(this.agent.getPing()!=null) {
				for(AID agent : this.agent.getPing().getTargetList()) {
					message.addReceiver(agent);
					//System.out.print("Receiver : "+agent);
				}
				message.setSender(this.agent.getAID());
				message.setContent(this.agent.getExplo().getMap().serialize());//we send serialised graph
				this.agent.sendMessage(message);
				System.out.println("Map send : "+message);
			}
		}
	}
	
	public int onEnd() {
		System.out.println("SEND END -> 0");
		return 0;
	}

}

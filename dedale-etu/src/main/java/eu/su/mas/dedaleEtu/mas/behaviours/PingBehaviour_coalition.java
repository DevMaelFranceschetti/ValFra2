package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.graphstream.graph.Graph;

import java.time.*;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.strategy.BossStrategy;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractExploAgent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Random;

public class PingBehaviour_coalition  extends OneShotBehaviour implements IPingBehaviour{
	
	/*
	 * Je retravaille là dessus pour faire des blocs de codes propres (modules) et avoir une gestion complète
	 * des communications.
	 * 
	 */

	private static final long serialVersionUID = -2058134622078521998L;

	private AbstractExploAgent agent;
	private ArrayList<AID> targetList;
	private List<String> agentPos;
	private HashMap<String,String> targetPos = new HashMap<>();
	private ArrayList<String> coalitionAgents;
	private HashMap<String,Integer> agentSeen = new HashMap<>();
	private ArrayList<String> lastCoalitionMembers = new ArrayList<>();
	private boolean joined = false;
	private double myRandom = 0.0;
	private double otherRandom = 0.0;
	private HashMap<String,Double> randomSend = new HashMap<>();
	private ArrayList<String> removedFromCoalition = new ArrayList<>();
	private String bossPosition = "";
	private String bossLastCoalition = "";
	private String bossLastCoalitionPos = "";
	private String oldBossPosition = "";
	private ArrayList<String> diffused = new ArrayList<>();//mémorise les pings déjà diffusés.
	private ArrayList<String> inviteRefused = new ArrayList<>();//pour ne pas réinviter un agent qui a refusé
	private int counterBossReturn = 0; //compteur de temps avant le retour au boss
	//paramètre :
	private int counterResetRefused = 0;
	private int bossReturnDelay = 20;//durée avant de retourner au boss, cf bossStrategy
	private int lost_agent_delay = 50;//durée à partir de laquelle on considère un agent "perdu", qui redevient autonome (évite des blocages)
	private boolean informBossLefted = false;
	
	public boolean needInformBossLefted() {
		return this.informBossLefted;
	}
	
	public String getOldBossPos() {
		return this.bossLastCoalitionPos;
	}
	
	public String getOldBossName() {
		return this.bossLastCoalition;
	}
	
	
	public void informBossMoving(String bossDestination) {
		if(!this.bossPosition.equalsIgnoreCase(bossDestination)) {
			//on doit diffuser l'info
			this.bossPosition = bossDestination;
		}
	}
	
	public int getBossReturnDelay() {
		return this.bossReturnDelay;
	}
	public void incrementCounterBossReturn() {
		this.counterBossReturn+=1;
	}
	
	public boolean needReturnBoss() {
		return this.counterBossReturn>bossReturnDelay;
	}
	
	private void resetBossReturnCounter() {
		this.counterBossReturn = 0;
	}
	
	private String coalitionBoss = "";
	private int joiningCoalition = 0;
	private ArrayList<String> agentsWaitings = new ArrayList<>();//mémorise les agents en cours d'inscription, pour aborder le processus si ils sont partisà cause d'un souci de délai d'envoi.
	
	public String getBossPosition() {
		return this.bossPosition;
	}
	
	public boolean isBoss() {
		return this.coalitionBoss.equalsIgnoreCase(this.agent.getName());
	}
	
	public ArrayList<String> getCoalitionAgents(){
		return this.coalitionAgents;
	}
	
	public boolean needWait() {
		return this.joiningCoalition>0;
	}
	
	
	public int utility(int coalitionSize) {//utilité du nouvel agent de 1 par agent de la coalition si inférieur au max, 1 sinon.
		int max_coalition_size = 5;
		int individual_utility = 1;
		return (coalitionSize<=max_coalition_size?coalitionSize*individual_utility:individual_utility);
	}

	public PingBehaviour_coalition(AbstractExploAgent myagent) {
		this.agent= myagent;
		this.targetList = new ArrayList<>(); //les agents avec qui on est en train de communiquer
		//les listes contenant toutes les infos reçues par message : 
		this.agentPos = new ArrayList<>();//position des agents autour
		this.coalitionAgents = new ArrayList<>();
		this.coalitionAgents.add(this.agent.getName());
	}
	
	public ArrayList<String> getAgentsAround() {
		return (ArrayList<String>)this.agentPos;
	}
	public ArrayList<AID> getTargetList() {
		return this.targetList;
	}
	
	public AMSAgentDescription[] getAgentsList() {
		//récupérer liste des agents
		AMSAgentDescription[] agents = null;
		try {
			SearchConstraints c = new SearchConstraints();
			c.setMaxResults(new Long(-1));
			agents = AMSService.search(this.agent,  new AMSAgentDescription(),c);
		}catch(Exception e) {
			System.err.println(e);
		}
		return agents;
	}
	
	public void sendInformationPing(String position, AMSAgentDescription[] destinataires) {
		/*
		 * Envoi du ping informatif aux destinataires
		 */
		if(position!=null ) {//si explo en cours
			ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
			for(int i = 0; i<destinataires.length;i++) {
				message.addReceiver(destinataires[i].getName());
			}
			message.setSender(this.agent.getAID());
			message.setContent("02:"+position);//envoie la case où il est, et où il veut aller
			this.agent.sendMessage(message);
		}
	}
	
	
	public void sendCoalitionUpdatePing(String AgentOrigin, String bossPosition) {
		/*
		 * Envoi de la liste des membres aux membres de la coalition (pour si un nouveau est rajouté)
		 */
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		for(int i = 0; i<this.coalitionAgents.size();i++) {
			String name = this.coalitionAgents.get(i).split("@")[0];
			message.addReceiver(new AID(name, AID.ISLOCALNAME));
		}
		message.setSender(this.agent.getAID());
		String coalitionMembers = String.join(",",this.coalitionAgents);
		message.setContent("08:"+coalitionMembers+"&&"+bossPosition+"&&"+AgentOrigin+"&&"+this.coalitionBoss);//envoie la liste des membres de la coalition
		this.agent.sendMessage(message);
	}
	
	public void informStench(String idStench) {
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		for(int i = 0; i<this.coalitionAgents.size();i++) {
			String name = this.coalitionAgents.get(i).split("@")[0];
			message.addReceiver(new AID(name, AID.ISLOCALNAME));
		}
		message.setSender(this.agent.getAID());
		message.setContent("99:"+idStench);//confirme quitter la coalition pour celle d'un autre boss
		this.agent.sendMessage(message);
	}
	
	public void informWumpus(String idWumpus) {
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		for(int i = 0; i<this.coalitionAgents.size();i++) {
			String name = this.coalitionAgents.get(i).split("@")[0];
			message.addReceiver(new AID(name, AID.ISLOCALNAME));
		}
		message.setSender(this.agent.getAID());
		message.setContent("66:"+idWumpus);//confirme quitter la coalition pour celle d'un autre boss
		this.agent.sendMessage(message);
	}
	
	public void announceLefting(AID agent, String agentLeft) {
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		message.addReceiver(agent);
		message.setSender(this.agent.getAID());
		message.setContent("09:"+agentLeft+"&&"+this.coalitionBoss);//confirme quitter la coalition pour celle d'un autre boss
		this.agent.sendMessage(message);
	}
	
	public void diffuseLefted(String agentLeft) {
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		for(int i = 0; i<this.coalitionAgents.size();i++) {
			String name = this.coalitionAgents.get(i).split("@")[0];
			message.addReceiver(new AID(name, AID.ISLOCALNAME));
		}
		message.setSender(this.agent.getAID());
		message.setContent("09:"+agentLeft);//confirme nouvelle position du boss
		this.agent.sendMessage(message);
	}
	
	public void rogerLefting(AID agent, String agentLeft) {
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		message.addReceiver(agent);
		message.setSender(this.agent.getAID());
		message.setContent("19:"+agentLeft);//confirme réception
		this.agent.sendMessage(message);
	}
	
	public void roger(AID sender) {//roger new boss position
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		message.addReceiver(sender);
		message.setSender(this.agent.getAID());
		message.setContent("18:"+this.bossPosition);//confirme nouvelle position du boss
		this.agent.sendMessage(message);
	}
	
	public void createCoalition() {
		//propose la création d'une coalition ou l'invitation dans la coalition existante.
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		Random rd = new Random();//tirage d'un random
		this.myRandom = rd.nextDouble();
		for(int i = 0; i<this.targetList.size();i++) {
			String toInvite = this.targetList.get(i).getName();
			if(!this.coalitionAgents.contains(toInvite) &&!inviteRefused.contains(toInvite)) {//on invite si l'agent n'est pas déjà dans la coalition ou n'a pas déjà refusé ou quitté la coalition //&&!this.removedFromCoalition.contains(toInvite)
				message.addReceiver(this.targetList.get(i));
				if(this.coalitionBoss.equalsIgnoreCase("")) {//on mémorise le random envoyé à ces agents
					this.randomSend.put(toInvite, this.myRandom);
				}
				System.out.println(this.agent.getName()+" - inviting :"+toInvite);
				this.agentsWaitings.add(toInvite);//on mémorise qu'on a une attente avec cet agent
			}
		}
		int size = this.coalitionAgents.size();
		message.setSender(this.agent.getAID());
		if(this.coalitionBoss.equalsIgnoreCase("")) {
			message.setContent("05:"+size+"&&random,"+this.myRandom);//envoie la demande de création / l'invitation dans la coalition
		}
		else {//on force le chef si existant
			message.setContent("05:"+size+"&&boss,"+this.coalitionBoss);//envoie la demande de création / l'invitation dans la coalition
		}
		this.agent.sendMessage(message);
	}
	
	public void joinCoalition(AID invite) {
		//veut rejoindre la coalition.
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		message.addReceiver(invite);
		message.setSender(this.agent.getAID());
		message.setContent("06:joining");
		this.agent.sendMessage(message);
	}
	
	public void refuseCoalition(AID invite) {
		//veut rejoindre la coalition.
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		message.addReceiver(invite);
		message.setSender(this.agent.getAID());
		message.setContent("16:refused");
		this.agent.sendMessage(message);
	}
	
	public void confirmCoalition(AID invited) {
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		message.addReceiver(invited);
		message.setSender(this.agent.getAID());
		String coalitionMembers = String.join(",",this.coalitionAgents);
		if(this.coalitionBoss.equalsIgnoreCase("")) {//si la coalition est nouvelle, on choisit le boss
			Collections.sort(this.coalitionAgents);
			this.coalitionBoss = this.coalitionAgents.get(0);
			// si c'est nous le boss : 
			if(this.coalitionBoss.equalsIgnoreCase(this.agent.getName())) {//si on est le boss, on sauvegarde notre position de boss
				this.bossPosition = this.agent.getCurrentPosition();
			}
			else{
				//sinon c'est l'autre membre :
				this.bossPosition = this.targetPos.get(this.coalitionBoss);
			}
		}
		message.setContent("07:"+coalitionMembers+"&&"+this.coalitionBoss+"&&"+this.bossPosition);//envoie la demande de création / l'invitation dans la coalition
		this.agent.sendMessage(message);
	}
	
	public void confirmRefusedCoalition(AID invited) {
		ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
		message.addReceiver(invited);
		message.setSender(this.agent.getAID());
		message.setContent("17:refused");//envoie la demande de création / l'invitation dans la coalition
		this.agent.sendMessage(message);
	}
	
	public void receivePings(String position, String destination) { // TOTO continue
		/*
		 * Reçoit les messages de tout type et les classe dans les bonnes listes selon le type
		 */
		MessageTemplate mt = MessageTemplate.MatchPerformative( ACLMessage.PROPOSE );
		ACLMessage msg = this.agent.receive(mt);
		while(msg!=null) {
			if(msg.getSender().getName()!=this.agent.getAID().getName()) {//si c'est un autre agent qui a envoyé le ping
				//traitement du message : 
				if(!this.targetList.contains(msg.getSender())) {
					this.targetList.add(msg.getSender());
				}
				String[] message_content = msg.getContent().split(":");
				String type = message_content[0];
				String agentPos;
				String agentName;
				String[] data;
				System.out.println(this.agent.getName()+" - Message received : "+msg.getContent());
				switch(type) {
					
					case "02": //info de position des autres agents
						data = message_content[1].split(",");
						agentPos = data[0];
						agentName = msg.getSender().getName();
						this.targetPos.put(agentName, agentPos);
						if(!this.agentPos.contains(agentPos)) {
							this.agentPos.add(agentPos);
						}
						if(isBoss()) {
							this.agentSeen.put(agentName, 0);//on a vu cet agent
						}
						System.out.println(this.agent.getName()+" - agent detected: "+msg.getContent());
						if(this.informBossLefted&&agentName.equalsIgnoreCase(this.bossLastCoalition)) {
							//si on croise notre ancien boss et qu'on ne l'a pas informé qu'on quittais sa coalition, on l'informe
							announceLefting(msg.getSender(), this.agent.getName());
						}
						break;
					case "05"://proposition de rejoindre une coalition
						if(message_content.length>1) {
							agentName = msg.getSender().getName();
							String[] content = message_content[1].split("&&");
							int coalitionSize = Integer.valueOf(content[0]);
							String[] boss_info = content[1].split(",");
							String boss;
							if(boss_info[0].equalsIgnoreCase("boss")) {//si la coalition a déjà un chef
								boss = boss_info[1];
							}else {//sinon on en élit un par tri ascii des noms (évite des messages pour des jets de dé)
								List<String> agents = new ArrayList<>();
								agents.add(this.agent.getName());
								agents.add(agentName);
								Collections.sort(agents);
								boss = agents.get(0);
							}
							//on est invité dans cette coalition
							System.out.println(this.agent.getName()+" - invited to coalition of boss : "+boss+" by : "+agentName);
							if(isBoss()&&boss.equalsIgnoreCase(agentName)) {
								System.out.println(this.agent.getName()+" - Chef de coalition a croisé un autre chef : "+boss);
							}
							this.agentsWaitings.add(agentName);//il faudra attendre un peu pour bien finaliser l'"inscription" ou le refus de cet agent
							if(!joined && !isBoss()&& utility(coalitionSize+1)>utility(this.coalitionAgents.size())) {//si on a pas de coalition et que c'est intéressant, on accepte  //&& this.coalitionAgents.size()==1
								joinCoalition(msg.getSender());//accepte la coalition
								this.joined = true;
							}
							else {
								refuseCoalition(msg.getSender());//refuse la coalition
							}
						}
						break;
					case "06":
						//joined
						agentName = msg.getSender().getName();
						System.out.println(this.agent.getName()+" - accepted to join my coalition : "+agentName);
						this.coalitionAgents.add(agentName);
						System.out.println(this.agent.getName()+" - confirming new coalition: "+this.coalitionAgents);
						confirmCoalition(msg.getSender());//on confirme
						this.agentsWaitings.remove(agentName);//fin de l'"inscription" de cet agent
						break;
					case "16":
						//refused invitation
						agentName = msg.getSender().getName();
						System.out.println(this.agent.getName()+" - refused to join my coalition : "+agentName);
						confirmRefusedCoalition(msg.getSender());//on confirme
						this.inviteRefused.add(agentName);//on mémorise qu'il a refusé pour ne pas le réinviter.
						this.agentsWaitings.remove(agentName);//fin de l'"inscription" de cet agent
						System.err.println(this.agent.getName()+" - confirming refused invitation of : "+agentName);
						break;
					case "07":
						//confirmed acceptation
						agentName = msg.getSender().getName();
						if(message_content.length>1) {
							String[] content = message_content[1].split("&&");
							String boss = content[1];
							String infoBossPosition = content[2];
							String[] agentsName = content[0].split(",");
							this.lastCoalitionMembers = (ArrayList<String>) this.coalitionAgents.clone();
							this.coalitionAgents = new ArrayList<>();
							for(String name : agentsName) {
								if(!this.coalitionAgents.contains(name)) {
									this.coalitionAgents.add(name);
								}
							}
							System.err.println(this.agent.getName()+" - confirmed coalition: boss="+boss+", position : "+infoBossPosition+", members="+this.coalitionAgents);
							if(!this.coalitionBoss.equalsIgnoreCase("")&&!this.coalitionBoss.equalsIgnoreCase(boss)) {
								//on avais une coalition, il faut informer le chef qu'on la quitte
								this.bossLastCoalition = this.coalitionBoss;
								this.bossLastCoalitionPos = this.bossPosition;
								if(!this.coalitionAgents.contains(this.bossLastCoalition)) {
									System.err.println(this.agent.getName()+" - changed coalition for "+boss+"'s one, has to inform old boss : "+this.bossLastCoalition);
									this.informBossLefted = true;//si l'ancien chef n'a pas aussi rejoint la nouvelle coalition, on doit l'informer qu'on quitte la sienne.
								}
							}
							this.bossPosition = infoBossPosition;//on mémorise la position du chef
							this.counterBossReturn = 0;//reset compteur boss
							this.counterResetRefused = 0; //reset compteur invitations refusées
							this.coalitionBoss = boss;
							this.joined = false;//on a finalisé l'inscription, on peut accepter une meilleure coalition plus tard
							//nouvelle coalition, on reset les infos en lien avec l'ancienne :
							this.inviteRefused = new ArrayList<>();
							this.removedFromCoalition = new ArrayList<>();
							this.agentsWaitings.remove(agentName);
						}
						break;
					case "17":
						//refused invitation confirmed
						agentName = msg.getSender().getName();
						this.agentsWaitings.remove(agentName);//fin de l'"inscription" de cet agent
						System.err.println(this.agent.getName()+" - confirmed refused invitation by: "+agentName);
						break;
					case "08":
						//update coalition members
						agentName = msg.getSender().getName();
						if(this.coalitionAgents.contains(agentName)) {//on vérifie mtn que l'update vient d'un membre de la coalition, si jamais on a quitté entre temps.
							if(message_content.length>1) {
								String[] coalitionInfos = message_content[1].split("&&");
								String[] agentsName = coalitionInfos[0].split(",");
								String bossPosition_next = coalitionInfos[1];
								String agentOrigin = coalitionInfos[2];
								String bossName = coalitionInfos[3];
								//corriger les incohérences de boss : on prend celui de plus petit nom en ordre ascii pour départager !
								if(!bossName.equalsIgnoreCase(this.coalitionBoss)) {
									System.out.println(this.agent.getName()+" - Error : different boss for same coalition !");
									List<String> agents = new ArrayList<>();
									agents.add(bossName);
									agents.add(this.coalitionBoss);
									Collections.sort(agents);
									String goodBoss = agents.get(0);
									if(!this.coalitionBoss.equalsIgnoreCase(goodBoss)) {
										this.bossPosition = bossPosition_next;//on corrige alors aussi la position du boss (forcément pas celle qu'on avais !)
										this.coalitionBoss = goodBoss;
										System.err.println(this.agent.getName()+" - Boss corrected : "+this.coalitionBoss+" at :"+this.bossPosition);
									}
								}
								System.out.println(this.agent.getName()+" - received bossPosition : "+bossPosition_next);
								//vérifier la source de l'info pour update notre info de la position du boss ou non (on considère que les autres agents sont pas fiables pour ça car ils peuvent avoir une ancienne info)
								if(agentName.equalsIgnoreCase(this.coalitionBoss)) {
									System.err.println(this.agent.getName()+" - boss informed next destination : "+bossPosition_next);
									this.oldBossPosition = this.bossPosition;
									this.bossPosition = bossPosition_next;
									roger(msg.getSender());//on informe qu'on a bien pris en compte l'info
								}
								else {
									if(!isBoss()&&!bossPosition_next.equalsIgnoreCase(this.bossPosition)&&!bossPosition_next.equalsIgnoreCase(this.oldBossPosition)) {
										this.oldBossPosition = this.bossPosition;
										this.bossPosition = bossPosition_next;
									}
								}
								for(String name : agentsName) {
									//on ajoute les nouveaux agents (s'ils n'ont pas quitté la coalition)
									if(!this.coalitionAgents.contains(name)&&!this.removedFromCoalition.contains(name)) {
										this.coalitionAgents.add(name);
										System.err.println(this.agent.getName()+" - updated coalition, added: "+name);
									}
									if(this.removedFromCoalition.contains(name)) {//si l'agent a quitté la coalition, on informe l'expéditeur
										System.err.println(this.agent.getName()+" - error in infos received, agent  "+name+" has left, informing the sender");
										announceLefting(msg.getSender(), name);//on informe que cet agent n'est plus dans la coalition
									}
								}
								//si on est le boss:
								if(isBoss()) {
									this.agentSeen.put(agentOrigin, 0);//on a eu des nouvelles de cet agent
									if(this.bossPosition.equalsIgnoreCase(bossPosition_next)) {//si l'agent à l'origine du ping a la bonne info, on considère qu'il est au courant 
										this.agent.getBossStrat().informed(agentOrigin);
										System.err.println(this.agent.getName()+" - Chef de coalition : confirmation agent "+agentOrigin+ " informé nouvelle destination (echo)");
									}
									else {
										System.err.println(this.agent.getName()+" - Chef de coalition : info erronée "+agentOrigin+ " sur la destination (echo)");	
									}
								}
								else {
									if(!this.diffused.contains(agentOrigin)) {
										this.diffused.add(agentOrigin);
										sendCoalitionUpdatePing(agentOrigin,this.bossPosition);//on relaye le ping d'update, façon flooding.
									}
								}
							}
							
						}
						break;
					case "18":
						//réception de la confirmation de prise en compte de la destination du boss
						if(isBoss()) {
							agentName = msg.getSender().getName();
							this.agent.getBossStrat().informed(agentName);
							System.err.println(this.agent.getName()+" - Chef de coalition : confirmation agent "+agentName+ " informé nouvelle destination");
						}
						break;
					case "09":
						//mémoriser que l'agent a quitté
						agentName = msg.getSender().getName();
						if(this.coalitionAgents.contains(agentName)) {
							String[] infosLeft = message_content[1].split("&&");
							if(infosLeft.length>0) {
								String leftAgent = infosLeft[0];
								boolean left = true;
								if(infosLeft.length>1) {
									String newBoss = infosLeft[1];
									if(this.coalitionAgents.contains(newBoss)) {
										left = false;
										System.err.println(this.agent.getName()+" - error boss name for agent "+leftAgent+", so he don't really left this coalition");
										rogerLefting(msg.getSender(), leftAgent);//on confirme réception même si il quitte pas vraiment...
									}
								}
								if(left){
									if(!this.removedFromCoalition.contains(leftAgent)) {
										this.removedFromCoalition.add(leftAgent);
									}
									if(this.coalitionAgents.contains(leftAgent)) {
										this.coalitionAgents.remove(leftAgent);
									}
									System.err.println(this.agent.getName()+" - informed agent "+leftAgent+" left coalition");
									rogerLefting(msg.getSender(), leftAgent);//on confirme réception
									if(isBoss()) {
										if(this.coalitionAgents.size()==1) {//Si on est le boss et tout seul (tous les agents ont quittés), on redevient seul.
											this.coalitionBoss = "";
											this.joined = false;
											this.inviteRefused = new ArrayList<>();
											this.removedFromCoalition = new ArrayList<>();
											this.bossPosition = "";
										}
									}
								}
							}
						}
						break;
					case "19":
						agentName = msg.getSender().getName();
						String leftedAgent = message_content[1];
						if(agentName.equalsIgnoreCase(this.bossLastCoalition)&&leftedAgent.equalsIgnoreCase(this.agent.getName())){
							System.err.println(this.agent.getName()+" - old boss "+agentName+" informed I lefted");
							this.informBossLefted = false;//le boss est informé, plus besoin de le faire
						}
						break;
					case "66":
						//wumpus detected
						if(message_content.length>0) {
							String wump = message_content[1];
							if(!this.agent.wumpusSeen()) {
								System.err.println(this.agent.getName()+" - informed WUMPUS detected at "+wump);
								this.agent.wumpusSeen(wump);
								//on ne tient compte de l'info que si on ne traque pas déjà un autre wumpus (pour éviter les girouettes)
							}
						}
						break;
					case "99":
						//stench detected
						if(message_content.length>0) {
							String stench = message_content[1];
							if(!this.agent.isStench(stench)) {
								System.err.println(this.agent.getName()+" - informed stench detected at "+stench);
								this.agent.putStench(stench);
							}
						}
						break;
					default :
						System.out.println(this.agent.getName()+" - Message type not recogniser : "+msg.getContent());
				}
			}
			//suivant :
			msg = this.agent.receive(mt);
		}
	}
	

	@Override
	public void action() {
		System.out.println(this.agent.getName()+" - Coalition : "+this.coalitionAgents+" , boss : "+this.coalitionBoss+" at : "+this.bossPosition);
		this.agent.incrementTime();//on incrémente le pas de temps de l'agent.
		boolean collision = false;
		if(this.agent.getSupposedPos()!=null && this.agent.getCurrentPosition()!=null ) {
			if(!this.agent.getCurrentPosition().equalsIgnoreCase(this.agent.getSupposedPos())) {
				//si l'agent ne s'est pas rendu sur la case prévue alors qu'il n'y avais pas de collision prévue
				System.out.println(this.agent.getName()+" - Collision imprévue : "+this.agent.getSupposedPos());
				collision = true;
			}
		}
		this.diffused = new ArrayList<>();
		this.joiningCoalition = 0;
		this.agentsWaitings = new ArrayList<>();
		this.agentPos = new ArrayList<>();
		this.targetPos = new HashMap<>();
		this.counterResetRefused+=1;
		if(this.counterResetRefused>=10) {//tous les 10 tours, on reset les invitations refusés, pour donner une seconde chance aux agents
			this.counterResetRefused = 0;
			this.inviteRefused = new ArrayList<>();
		}
		if(isBoss()) {
			//on considère tous les agents ayant dépassé le délai comme étant perdus (ont quitté la coalition)
			ArrayList<String> toRemove = new ArrayList<>();
			for(String agent : this.agentSeen.keySet()) {
				if(!agent.equalsIgnoreCase(this.agent.getName())) {//on évite de se virer soi-même, c'est mieux...
					int oldValue = this.agentSeen.get(agent);
					if(oldValue > this.lost_agent_delay) {
						if(!this.removedFromCoalition.contains(agent)) {
							this.removedFromCoalition.add(agent);
						}
						if(this.coalitionAgents.contains(agent)) {
							this.coalitionAgents.remove(agent);
						}
						toRemove.add(agent);
						System.err.println(this.agent.getName()+" - implicitly, agent "+agent+" left coalition");
					}
					else {
						this.agentSeen.put(agent, oldValue+1);//incrémente le délai depuis lequel on a aps vu cet agent
					}
				}
			}
			for(String remAgent : toRemove) {
				this.agentSeen.remove(remAgent);
			}
			if(this.coalitionAgents.size()==1) {//Si on est le boss et tout seul (tous les agents ont quittés), on redevient sans coalition.
				this.coalitionBoss = "";
				this.joined = false;
				this.inviteRefused = new ArrayList<>();
				this.removedFromCoalition = new ArrayList<>();
				this.bossPosition = "";
			}
		}
		else {
			if(this.counterBossReturn>this.lost_agent_delay && !this.agent.wumpusSeen()) {//si agent perdu et pas en traque
				//agent perdu, quitte la coalition : 
				this.coalitionAgents = new ArrayList<>();
				this.coalitionAgents.add(this.agent.getName());
				this.coalitionBoss = "";
				this.joined = false;
				this.inviteRefused = new ArrayList<>();
				this.removedFromCoalition = new ArrayList<>();
				this.bossPosition = "";
				this.counterBossReturn = 0;
				System.err.println(this.agent.getName()+" - lost agent (can't return to boss for too much time), leave implicitly coalition");
			}
		}
		if(this.coalitionAgents.size()==1) {
			this.joined=false;//on réinitialise les invitations acceptées si l'agent n'a toujours pas de coalition, pour éviter des problèmes de délai.
		}
		AMSAgentDescription[] destinataires = getAgentsList();
		if(destinataires!=null) {//si il y a des agents à qui envoyer
			String position = this.agent.getCurrentPosition();
			
			//envoi du ping "sonnar", informatif sur la position :
			sendInformationPing(position, destinataires);
			if(this.coalitionAgents.size()>1) {
				sendCoalitionUpdatePing(this.agent.getName(),this.bossPosition);//on relaye le ping d'update, façon flooding.//update de la liste des membres (systématiques, plus simple)
				for(String agentLefted : this.removedFromCoalition) {
					diffuseLefted(agentLefted);//on diffuse l'info des agents qui ont quitté la coalition, façon flooding.
				}
				for(String stench : this.agent.getStench()) {
					informStench(stench);//informe de l'odeur trouvée 
				}
				if(this.agent.wumpusSeen()) {//informe du dernier wumpus vu !
					informWumpus(this.agent.lastWumpusSeen());
				}
			}
			
			try {//attente avant réception
				this.agent.doWait(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//on reçoit et traite tous les messages reçus
			receivePings(position, null);
			
			try {//attente avant réception
				this.agent.doWait(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
			createCoalition();
			 
			try {//attente avant réception
				this.agent.doWait(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//on reçoit et traite tous les messages reçus
			receivePings(position, null);
			
			try {//attente avant réception
				this.agent.doWait(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//on reçoit et traite tous les messages reçus
			receivePings(position, null);
			
			//si on a une coalition et qu'on a vu le chef, alors on réinitialise le compteur de temps depuis la dernière visite au boss
			if(this.coalitionAgents.size()>1 && !this.isBoss()) {
				this.incrementCounterBossReturn();
				if(this.targetPos.containsKey(this.coalitionBoss)) {
					this.resetBossReturnCounter();
					System.out.println(this.agent.getName()+" - boss croisé !");
				}
				else {
					if(this.needReturnBoss()) {
						System.out.println(this.agent.getName()+" - doit retourner au boss !");
					}
				}
			}
			
		}
		if(collision){
			//si il y avais une collision
			if(this.agentPos.contains(this.agent.getSupposedPos())) {
				System.out.println(this.agent.getName()+" - Collision était due à : Agent ");
			}
			else {
				if(this.agent.isStench(this.agent.getSupposedPos())) {//il y a forcément de l'odeur sur la case s'il y a ou y avais le wumpus au step précédent
					System.err.println(this.agent.getName()+" - Collision était due à : Wumpus en "+this.agent.getSupposedPos());
					this.agent.wumpusSeen(this.agent.getSupposedPos()); //on mémorise qu'on a trouvé un wumpus ici !
				}
				else {
					System.out.println(this.agent.getName()+" - Collision était due à : Agent ");
				}
			}
		}
		
		ArrayList<String> toRemove = new ArrayList<>();
		for(String agentWaiting : this.agentsWaitings) {
			if(!this.targetPos.containsKey(agentWaiting)) {
				//l'agent en question n'est plus autour de nous, aborder la communication
				toRemove.add(agentWaiting);
			}
		}
		this.agentsWaitings.removeAll(toRemove);//aborder les communications interrompues
		this.joiningCoalition = this.agentsWaitings.size();//on mémorise combien d'agents sont en attente
		System.out.println(this.agent.getName()+" - waitings remaining:"+this.joiningCoalition);
	}
	
	public int onEnd() {
		if(this.targetList.size()>0) { //si on a détecté des agents autour
			return 1;
		}
		return 0;
	}

	@Override
	public boolean hasCoalition() {
		return this.coalitionAgents.size()>1;
	}




}

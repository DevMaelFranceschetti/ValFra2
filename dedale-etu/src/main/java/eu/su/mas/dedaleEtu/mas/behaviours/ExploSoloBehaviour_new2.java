package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractExploAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.strategy.BossStrategy;
import eu.su.mas.dedaleEtu.mas.strategy.ExploSoloStrategy;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;


/**
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.</br>
 * 
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs.</br> 
 * This (non optimal) behaviour is done until all nodes are explored. </br> 
 * 
 * Warning, this behaviour does not save the content of visited nodes, only the topology.</br> 
 * Warning, this behaviour is a solo exploration and does not take into account the presence of other agents (or well) and indefinitely tries to reach its target node
 * @author hc
 *
 */
public class ExploSoloBehaviour_new2 extends OneShotBehaviour implements IExploBehaviour{

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;
	private String nextNode = null;
	public boolean letGo = false;
	private AbstractExploAgent agent;
	private String bossDestination = "";//ne pas utiliser si on est pas le chef de la coalition
	private boolean startedMoving = false;
	
	private boolean isMoving() {
		return startedMoving&&!bossDestination.equalsIgnoreCase("")&&!this.agent.getCurrentPosition().equalsIgnoreCase(bossDestination);
	}
	
	public String getNextNode() {
		return this.nextNode;
	}
	
	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	/**
	 * Nodes known but not yet visited
	 */
	private ArrayList<String> openNodes;
	/**
	 * Visited nodes
	 */
	private Set<String> closedNodes;
	
	private List<String> lastNodes;
	
	public MapRepresentation getMap() {
		return this.myMap;
	}

	public List<String> getLastMoves() {
		return this.lastNodes;
	}
	
	public void addOpenNode(String node) {
		if (!this.openNodes.contains(node)&&!this.closedNodes.contains(node)){
			this.openNodes.add(node);
		}
	}
	
	public void removeOpenNode(String node) {
		if(this.openNodes.contains(node)) {
			this.openNodes.remove(node);
		}
		if(!this.closedNodes.contains(node)) {
			this.closedNodes.add(node);
		}
	}

	public ExploSoloBehaviour_new2(final AbstractExploAgent myagent) {
		super(myagent);
		this.agent = myagent;
		this.myMap=myagent.getMap();
		this.openNodes=new ArrayList<String>();
		this.closedNodes=new HashSet<String>();
		this.letGo = false;
		this.lastNodes = new ArrayList<String>();
		
	}

	@Override
	public void action() {
		if(this.myMap==null) {
			this.myMap= new MapRepresentation(this);
		}
		String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
	
		if (myPosition!=null){
			
			
			//on retire le noeud sur lequel on se trouve de ceux des wumpus rencontrés s'il y en avais, car il n'y est plus :
			this.agent.removeWumpus(this.agent.getCurrentPosition());
			for(String pos : this.agent.getPing().getAgentsAround()) {//les agents autour ne peuvent pas être sur un wumpus
				this.agent.removeWumpus(pos);
			}
			/*if(this.agent.hasCoalition()) {//si le wumpus qu'on chassais n'est plus là :
				if(this.agent.getCoalitionTarget().equalsIgnoreCase(this.agent.getCurrentPosition())){
					this.agent.leaveCoalition();//on quitte la coalition
				}
			}*/
			
			ArrayList<String> nearest = null;
			ArrayList<String> agentsAround = (ArrayList<String>) this.agent.getPing().getAgentsAround().clone();
			//List of observable from the agent's current position
			List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(800);
			} catch (Exception e) {
				e.printStackTrace();
			}

			//1) remove the current node from openlist and add it to closedNodes.
			this.closedNodes.add(myPosition);
			this.openNodes.remove(myPosition);

			this.myMap.addNode(myPosition,MapAttribute.closed);

			//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
			this.nextNode= null;
			Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
			while(iter.hasNext()){
				Couple<String, List<Couple<Observation, Integer>>> observed = iter.next();
				String nodeId=observed.getLeft();
				// on recherche la présence d'odeur autour de soi : 
				List<Couple<Observation, Integer>> infos = observed.getRight();
				if(infos.size()==0) {
					this.agent.noStench(nodeId);
				}
				for(Couple<Observation, Integer> i : infos) {
					if(i.getLeft() == Observation.STENCH) {
						this.agent.putStench(nodeId);
					}
				}
				
				if (!this.closedNodes.contains(nodeId)){
					if (!this.openNodes.contains(nodeId)){
						this.openNodes.add(nodeId);
						this.myMap.addNode(nodeId, MapAttribute.open);
						this.myMap.addEdge(myPosition, nodeId);	
					}else{
						//the node exist, but not necessarily the edge
						this.myMap.addEdge(myPosition, nodeId);
					}
					if (nextNode==null) nextNode = nodeId;
				}
			}
			
			//priorité aux noeuds avec de l'odeur :
			boolean informedStench = true;
			if(this.agent.getStench().size()>0 && informedStench) {
				System.err.println(this.agent.getName()+" - Stench : "+this.agent.getStench());
				nearest = this.myMap.getNearestTargetPathAvailable(myPosition, this.agent.getStench(), agentsAround);
				if(nearest!=null && nearest.size()>0) {
					nextNode= nearest.get(0);
					System.err.println(this.agent.getName()+" - Priority to stench node : "+nextNode);
				}else {
					System.err.println(this.agent.getName()+" - no path to stench node found ");
				}
			}
				
			
				if(this.agent.getPing().needWait()) {
					nextNode = this.agent.getCurrentPosition();
					System.out.println(this.agent.getName()+" : attend de finaliser son inscription de coalition, sur place.");
				}
				else {
					//Si l'agent est un chef de coalition :
					if(this.agent.getPing().isBoss()) {
						BossStrategy strat = this.agent.getBossStrat();
						ArrayList<String> members = (ArrayList<String>) this.agent.getPing().getCoalitionAgents().clone();
						members.remove(this.agent.getName());
						strat.setCoalitionMembers(members);//update la liste des membres de la coalition
						strat.incrementTime();
						
						//si on est en train de se déplacer vers notre nouvelle position :
						if(isMoving()) {
							//se déplacer d'un pas vers la destination
							System.out.println(this.agent.getName()+" - Chef de coalition : se déplace");
							ArrayList<String> bossMove= myMap.getTargetPath(this.agent.getCurrentPosition(),this.bossDestination, agentsAround);
							System.out.println(this.agent.getName()+" - bossMove : "+bossMove+", target: "+this.bossDestination+", pos :"+this.agent.getCurrentPosition()+" obs :"+String.join(",", agentsAround));
							if(bossMove!=null&&bossMove.size()>0) {
								nextNode = bossMove.get(0);
							}
							else {
								nextNode = this.agent.getCurrentPosition();
								System.out.println(this.agent.getName()+" - Chef de coalition : no actual way to move to destination, waiting");
							}
						}
						else {
							if(startedMoving&& this.agent.getCurrentPosition().equalsIgnoreCase(this.bossDestination)) {
								//fini de bouger, on réinitialise les infos sur le mouvement
								this.startedMoving = false;
								strat.moved();
								System.out.println(this.agent.getName()+" - MOVED TO END !");
							}
							if(strat.needToMove()&&!this.startedMoving) {
								if(this.bossDestination.equalsIgnoreCase("")||this.bossDestination.equalsIgnoreCase(this.agent.getCurrentPosition())) {
									System.out.println(this.agent.getName()+" - Chef de coalition : choisit sa destination");
									this.startedMoving = false;
									
									int bossDistanceMove= 2; //fixé arbitrairement
									//choisir case voisine comme destination : 
									ArrayList<String> nodes = (ArrayList<String>) this.openNodes.clone();
									for(String node : this.closedNodes) {
										nodes.add(node);
									}
									//DONE : destination à 1 ou 2 noeuds max à chaque fois pour éviter les blocages du boss en chemin et la perte de communication dans la coalition
									this.bossDestination = myMap.getTargetAvailableAtDistanceMax(this.agent.getCurrentPosition(), nodes, agentsAround, bossDistanceMove);
									if(this.bossDestination.equalsIgnoreCase("")) {
										System.out.println(this.agent.getName()+" - failed founding path");
										this.bossDestination = this.agent.getCurrentPosition();
									}
									System.out.println(this.agent.getName()+" - actual : "+this.agent.getCurrentPosition()+", destination : "+this.bossDestination);
									
									//DONE : pas gardé le modèle qui allais loin, car on perdait la communication si le boss se bloquais en chemin
									//choisir destination à une distance X=10
									//this.bossDestination = myMap.getTargetAvailableAtDistance(this.agent.getCurrentPosition(),this.openNodes, agentsAround, bossDistanceMove);
									/*if(this.bossDestination.equalsIgnoreCase("")) {
										System.out.println(this.agent.getName()+" - pas trouvé nouveau spot à >="+bossDistanceMove+", cherche à >="+bossDistanceMove/2+"...");
										//si on ne trouve aucune destination ouverte à une distance de 10, on essaye avec 5 :
										this.bossDestination = myMap.getTargetAvailableAtDistance(this.agent.getCurrentPosition(),this.openNodes, agentsAround, bossDistanceMove/2);
										//si on ne trouve rien même avec 5, alors on bouge à un noeud voisin de façon aléatoire (peut permettre de débloquer certains situations)
										if(this.bossDestination.equalsIgnoreCase("")) {
											System.out.println(this.agent.getName()+" - pas trouvé nouveau spot à >="+bossDistanceMove/2+", cherche à >="+bossDistanceMove/4+"...");
											//si on ne trouve aucune destination ouverte à une distance de 10, on essaye avec 5 :
											this.bossDestination = myMap.getTargetAvailableAtDistance(this.agent.getCurrentPosition(),this.openNodes, agentsAround, bossDistanceMove/4);
											//si on ne trouve rien même avec 5, alors on bouge à un noeud voisin de façon aléatoire (peut permettre de débloquer certains situations)
											if(this.bossDestination.equalsIgnoreCase("")) {
												System.out.println(this.agent.getName()+" - pas trouvé nouveau spot à >="+bossDistanceMove/2+", prend le plus proche noeud ouvert");
												this.bossDestination = myMap.getNearestTargetAvailable(this.agent.getCurrentPosition(),this.openNodes, agentsAround);
											}
										}
									}*/
									
									//informer à partir de maintenant les agents qu'on va se déplacer prochainement
									this.agent.getPing().informBossMoving(this.bossDestination);
									nextNode = this.agent.getCurrentPosition();
								}
								else {
									if(!strat.allInformed()) {
										System.out.println(this.agent.getName()+" - Chef de coalition : informe les agents avant de se déplacer");
										//continuer d'informer les agents qu'on va se déplacer prochainement
										nextNode = this.agent.getCurrentPosition();
									}
									else {
										System.out.println(this.agent.getName()+" - Chef de coalition : se déplace");
										this.startedMoving = true;
										//se déplacer d'un pas vers la destination
										ArrayList<String> bossMove= myMap.getTargetPath(this.agent.getCurrentPosition(),this.bossDestination, agentsAround);
										System.out.println(this.agent.getName()+" - bossMove : "+bossMove+", target: "+this.bossDestination+", pos :"+this.agent.getCurrentPosition()+" obs :"+String.join(",", agentsAround));
										if(bossMove!=null&&bossMove.size()>0) {
											nextNode = bossMove.get(0);
										}
										else {
											if(!this.agent.getCurrentPosition().equalsIgnoreCase(this.bossDestination)) {
												Random r= new Random();
												int moveId=1+r.nextInt(lobs.size()-1);
												String next = lobs.get(moveId).getLeft();
												nextNode = next;//random pos around
												System.out.println(this.agent.getName()+" - Chef de coalition : no actual way to move to destination, random");
											}else {
												System.out.println(this.agent.getName()+" - Chef de coalition : at destination, no moving");
											}
										}
									}
								}
								
							}
							else {
								System.out.println(this.agent.getName()+" - Chef de coalition : ne bouge pas pour le moment");
								nextNode = this.agent.getCurrentPosition();
							}
						}
					}
					//si l'agent n'est pas un chef de coalition :
					else {
						//si l'agent doit retourner à son chef de coalition (s'il en a une)
						if(this.agent.getPing().needInformBossLefted()) {
							String oldbossPos = this.agent.getPing().getOldBossPos();
							ArrayList<String> pathToBoss= myMap.getTargetPath(this.agent.getCurrentPosition(),oldbossPos, agentsAround);
							System.out.println(this.agent.getName()+" - path to old boss : "+pathToBoss);
							if(pathToBoss!=null && pathToBoss.size()>0) {
								nextNode = pathToBoss.get(0);
							}
							else {
								Random r= new Random();
								int moveId=1+r.nextInt(lobs.size()-1);
								String next = lobs.get(moveId).getLeft();
								nextNode = next;//random pos around
								System.out.println(this.agent.getName()+" - no actual way to return to old boss, random");
							}
							System.out.println(this.agent.getName()+" - Retourne à l'ancien boss...");
						}
						else {
							//on retourne au boss que si on est pas en train de bloquer un wumpus pour le moment
							if(this.agent.getPing().hasCoalition()&&this.agent.getPing().needReturnBoss()&&!this.agent.wumpusSeen()) {
								String bossPos = this.agent.getPing().getBossPosition();
								ArrayList<String> pathToBoss= myMap.getTargetPath(this.agent.getCurrentPosition(),bossPos, agentsAround);
								System.out.println(this.agent.getName()+" - path to boss : "+pathToBoss);
								if(pathToBoss!=null && pathToBoss.size()>0) {
									nextNode = pathToBoss.get(0);
								}
								else {
									Random r= new Random();
									int moveId=1+r.nextInt(lobs.size()-1);
									String next = lobs.get(moveId).getLeft();
									nextNode = next;//random pos around
									System.out.println(this.agent.getName()+" - no actual way to return to boss, random");
								}
								if(this.agent.getPing().needReturnBoss()) {
									System.err.println(this.agent.getName()+" - need regular return boss");
								}
								System.out.println(this.agent.getName()+" - Retourne au boss...");
								
							}
							else {
								//tout autre cas : exploration comme si solo.
								//USING SOLO STRATEGY TO SELECT ACTION
								ExploSoloStrategy strat = this.agent.getExploSoloStrategy();
								agentsAround.add(this.agent.getPing().getBossPosition());//on mémorise la position du boss (obstacle durant l'explo)
								nextNode = strat.getNextTarget(lobs, nextNode,this.agent, myMap, this);
							
							}
						}
					}
				}
			//}

			
			
			
			/***************************************************
			** 		ADDING the API CALL to illustrate their use **
			*****************************************************/

			//list of observations associated to the currentPosition
			List<Couple<Observation,Integer>> lObservations= lobs.get(0).getRight();
			//System.out.println(this.myAgent.getLocalName()+" - State of the observations : "+lobs);
			
			//example related to the use of the backpack for the treasure hunt
			Boolean b=false;
			for(Couple<Observation,Integer> o:lObservations){
				switch (o.getLeft()) {
				case DIAMOND:case GOLD:

					System.out.println(this.myAgent.getLocalName()+" - My treasure type is : "+((AbstractDedaleAgent) this.myAgent).getMyTreasureType());
					System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
					System.out.println(this.myAgent.getLocalName()+" - My expertise is: "+((AbstractDedaleAgent) this.myAgent).getMyExpertise());
					System.out.println(this.myAgent.getLocalName()+" - I try to open the safe: "+((AbstractDedaleAgent) this.myAgent).openLock(Observation.GOLD));
					System.out.println(this.myAgent.getLocalName()+" - Value of the treasure on the current position: "+o.getLeft() +": "+ o.getRight());
					System.out.println(this.myAgent.getLocalName()+" - The agent grabbed : "+((AbstractDedaleAgent) this.myAgent).pick());
					System.out.println(this.myAgent.getLocalName()+" - the remaining backpack capacity is: "+ ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
					b=true;
					break;
				default:
					break;
				}
			}

			//If the agent picked (part of) the treasure
			if (b){
				List<Couple<String,List<Couple<Observation,Integer>>>> lobs2=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
				System.out.println(this.myAgent.getLocalName()+" - State of the observations after picking "+lobs2);
				
				//Trying to store everything in the tanker
				System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());
				System.out.println(this.myAgent.getLocalName()+" - The agent tries to transfer is load into the Silo (if reachable); succes ? : "+((AbstractDedaleAgent)this.myAgent).emptyMyBackPack("Silo"));
				System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());
				
			}
			
			//Trying to store everything in the tanker
			//System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());
			//System.out.println(this.myAgent.getLocalName()+" - The agent tries to transfer is load into the Silo (if reachable); succes ? : "+((AbstractDedaleAgent)this.myAgent).emptyMyBackPack("Silo"));
			//System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());


			/************************************************
			 * 				END API CALL ILUSTRATION
			 *************************************************/
			if(!this.lastNodes.contains(this.nextNode)) {//we save the 10 last moves
				this.lastNodes.add(this.nextNode);
				if(this.lastNodes.size()>10) {
					this.lastNodes.remove(0);
				}
			}
			if(this.nextNode==myPosition) {
				System.out.println(this.myAgent.getName()+" -  SUR PLACE");
			}
			this.agent.setSupposedPos(this.nextNode);
			((AbstractDedaleAgent)this.myAgent).moveTo(this.nextNode);
		}

	}
	
	public int onEnd() {
		return 0;
	}

	@Override
	public ArrayList<String> getOpenNodes() {
		return this.openNodes;
	}


}

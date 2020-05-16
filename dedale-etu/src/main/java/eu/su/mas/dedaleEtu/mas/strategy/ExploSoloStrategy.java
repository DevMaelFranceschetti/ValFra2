package eu.su.mas.dedaleEtu.mas.strategy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractExploAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.IExploBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.IPingBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

public class ExploSoloStrategy implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int number_turns_locked = 0;
	private String locked_node = "";
	private String abandoned_wumpus = "";
	private int abandoned_timeout = 0;
	
	public String getNextTarget(List<Couple<String,List<Couple<Observation,Integer>>>> lobs, String nextNode,AbstractExploAgent agent, MapRepresentation map, IExploBehaviour explo) {
		ArrayList<String> nearest = null;
		ArrayList<String> openNodes = explo.getOpenNodes();
		ArrayList<String> agentsAround = agent.getPing().getAgentsAround();
		String myPosition = agent.getCurrentPosition();
		//si on traque un wumpus, qu'on a pas réussi à former de coalition, et que le seul noeud ouvert est le wumpus, on y va tout seul
		if (agent.wumpusSeen() && !agent.getPing().hasCoalition() && (openNodes.isEmpty() || (openNodes.size()==1 && openNodes.contains(agent.lastWumpusSeen())))){
			//Explo finished
			//Cas si wumpus vu mais pas de coalition et tout exploré ou pas possible d'explorer (bloqué par le wumpus)
			System.err.println(agent.getName()+" - RECRUITING FOR COALITION FAILED (no agent found)");
			System.err.println(agent.getName()+" - GOING ALONE TO WUMPUS");
			nearest = map.getTargetPath(myPosition,agent.lastWumpusSeen(), agentsAround);//calcule un trajet au wumpus
			System.out.println(agent.getName()+" - Nearest found : "+nearest);
			if(nearest==null || nearest.size()==0) {
				//si pas de noeud ouvert accessible...
				System.out.println(agent.getName()+" -  SUR PLACE, Noeud ouvert le plus proche dispo pas trouvé");
				nextNode = myPosition;//on attends que l'autre bouge. Vérifier que ça entraine pas des blocages
			}
			else {
				System.out.println(agent.getName()+" -  nextNode : "+nextNode);
				nextNode= nearest.get(0);
			}
		}else{
			
			if (nextNode==null || agent.isCollision() || agent.wumpusSeen()){//si on doit définir le prochain déplacement, qu'on a trouvé le wumpus ou qu'il y a collision
				
				System.out.println(agent.getName()+" - agents around : "+agentsAround.toString());
				
				if(agent.wumpusSeen() && !agent.getPing().hasCoalition()) {//CAS si wumpus vu mais pas de coalition
					System.err.println(agent.getName()+" - RECRUITING FOR COALITION");
					//On veut recruter : pour le moment, on explore en espérant trouver un agent
					//si notre seule issue n'est pas le noeud occupé par le wumpus
					ArrayList<String> obstacles = (ArrayList<String>) agentsAround.clone();
					obstacles.add(agent.lastWumpusSeen());//on ajoute l'obstacle du wumpus
					nearest = map.getNearestTargetPathAvailable(myPosition, openNodes, obstacles);//calcule un trajet au plus proche noeud ouvert accessible en tenant compte des obstacles et du wumpus
					if(nearest==null) {//pas de trajet trouvé car le wumpus gène
						//on a pas d'autre choix que d'avancer sur la position du wumpus, on le fait pour pouvoir détecter s'il se déplace
						System.out.println(agent.getName()+" - NO OTHER WAYS, TRYNING TO GO THROUGH WUMPUS");
						nearest = map.getNearestTargetPathAvailable(myPosition, openNodes, agentsAround);//calcule un trajet au plus proche noeud ouvert accessible en tenant compte des obstacles
					}
					System.out.println(agent.getName()+" - Nearest found : "+nearest);
				}
				else {
					/*if(agent.hasCoalition()&&!agent.getPing().hasCoalition()) {//CAS si on a uniquement une coalition de traque de wumpus ( = on a un wumpus à attraper)
						System.err.println(agent.getName()+" - GOING TO WUMPUS");
						//On veut aller au wumpus ciblé par la coalition:
						if(agentsAround.size()==0) {
							nearest = (ArrayList<String>)map.getShortestPath(myPosition, agent.getCoalitionTarget());
						}
						else {
							nearest = map.getTargetPath(myPosition, agent.getCoalitionTarget(), agentsAround);//calcule un trajet au wumpus en tenant compte des obstacles
						}
						if(nearest==null) {
							if(agent.getCurrentPosition()==this.locked_node) {
								this.number_turns_locked+=1;
							}
							else {
								this.number_turns_locked=1;
								this.locked_node = agent.getCurrentPosition();
							}
							if(this.number_turns_locked<5) {
								System.out.println(agent.getName()+" - No wumpus path found, trying again for precaution ");
							}else {
								System.out.println(agent.getName()+" - No wumpus path found, locked too much time, return to exploration ");
								//on quitte la coalition
								agent.removeWumpus(agent.getCoalitionTarget());//on oublie ce wumpus pour ne pas resté agglutiné inutlement
								agent.leaveCoalition();
							}
						}else {
							this.number_turns_locked=0;
							System.out.println(agent.getName()+" - Wumpus path found : " + nearest);
						}
					}*/
					//else {
						boolean wumpusKnown = false;
						if(agent.getPing().hasCoalition()&&agent.wumpusSeen()&&(!abandoned_wumpus.equalsIgnoreCase(agent.lastWumpusSeen())||abandoned_timeout>10)){
							System.out.println(agent.getName()+" - trying again to go to wumpus : "+ agent.lastWumpusSeen());
							abandoned_timeout = 0;
							abandoned_wumpus = "";
							wumpusKnown= true;
							//TODO : si on est sur une case avec de l'odeur et qu'on a pas vu de golem,
							// ne se déplacer que vers les noeuds avec de l'odeur voisins (essayer de trouver le golem)
							System.err.println(agent.getName()+" - has coalition and wumpus seen, going to wumpus");
							nearest = map.getTargetPath(myPosition, agent.lastWumpusSeen(), agentsAround);//calcule un trajet au wumpus en tenant compte des obstacles
							System.out.println(agent.getName()+" - nearest wumpus : "+nearest);
						}
						//Cas si on a pas de coalition ni vu de wumpus ou qu'on ne peut pas y aller
						//On veut aller au plus proche noeud ouvert : 
						if(wumpusKnown==false || nearest == null || nearest.size()==0) {
							boolean doExplo = true;
							abandoned_timeout+=1;
							if(wumpusKnown) {
								doExplo = false;
								if(agent.getCurrentPosition().equalsIgnoreCase(this.locked_node) && !map.isConnex(agent.getCurrentPosition(),agent.lastWumpusSeen())) { 
									this.number_turns_locked+=1;
								}
								else {
									this.number_turns_locked=1;
									this.locked_node = agent.getCurrentPosition();
								}
								if(this.number_turns_locked>5) {
									if(agent.getExplo().getOpenNodes().size()>1) {
										abandoned_wumpus = agent.lastWumpusSeen();
										System.out.println(agent.getName()+" -  no way to go to wumpus, locked from too much tries, explore around to find a way to this wumpus target");
									}
									else{
										agent.removeWumpus(agent.lastWumpusSeen());//on abandonne la traque de ce wumpus
										System.out.println(agent.getName()+" -  no way to go to wumpus, locked from too much tries, abandon this wumpus target");
									}
									doExplo = true;
								}
								if(this.number_turns_locked>10) {
									agent.removeWumpus(agent.lastWumpusSeen());//on abandonne la traque de ce wumpus
									System.out.println(agent.getName()+" -  no way to go to wumpus, locked from too much tries, abandon this wumpus target");
								}
								System.out.println(agent.getName()+" -  no way to go to wumpus, locked from "+this.number_turns_locked+" tries");
							}
							if(doExplo) {
								ArrayList<String> obstacles = (ArrayList<String>) agentsAround.clone();
								obstacles.add(agent.lastWumpusSeen());//on ajoute l'obstacle du wumpus
								nearest = map.getNearestTargetPathAvailable(myPosition, openNodes, obstacles);//calcule un trajet au plus proche noeud ouvert accessible en tenant compte des obstacles
								if(nearest==null || nearest.size()==0) {
									//si pas de noeud ouvert accessible...
									System.out.println(agent.getName()+" -  RONDE");
									//random move around :
									Random r= new Random();
									int moveId=1+r.nextInt(lobs.size()-1);
									String next = lobs.get(moveId).getLeft();
									nearest = new ArrayList<>();
									nearest.add(next);
								}
								else {
									System.out.println(agent.getName()+" - EXPLORATION");
								}
								System.out.println(agent.getName()+" - Nearest found : "+nearest);
							}
						}
					
					//}	
				}
				if(nearest==null || nearest.size()==0) {
					//si pas de noeud ouvert accessible...
					System.out.println(agent.getName()+" -  SUR PLACE, Noeud ouvert le plus proche dispo pas trouvé");
					nextNode = myPosition;//on attends que l'autre bouge. Vérifier que ça entraine pas des blocages
				}
				else {
					nextNode= nearest.get(0);
				}
			}
			else {
				System.out.println(agent.getName()+" : NextNode not null and no collision");
			}
		}
		return nextNode;
	}
}

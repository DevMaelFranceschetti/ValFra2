package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javafx.application.Platform;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.CloseFramePolicy;

import dataStructures.serializableGraph.*;
import eu.su.mas.dedaleEtu.mas.behaviours.IExploBehaviour;

/**
 * <pre>
 * This simple topology representation only deals with the graph, not its content.
 * The knowledge representation is not well written (at all), it is just given as a minimal example.
 * The viewer methods are not independent of the data structure, and the dijkstra is recomputed every-time.
 * </pre>
 * @author hc
 */
public class MapRepresentation implements Serializable {

	/**
	 * A node is open, closed, or agent
	 * @author hc
	 *
	 */

	public enum MapAttribute {
		agent,open,closed,territory, received
	}
	

	private static final long serialVersionUID = -1333959882640838272L;
	private IExploBehaviour explo;
	
	/*********************************
	 * Parameters for graph rendering
	 ********************************/

	private String defaultNodeStyle= "node {"+"fill-color: black;"+" size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private String nodeStyle_agent = "node.agent {"+"fill-color: forestgreen;"+"}";
	private String nodeStyle_open = "node.open {"+"fill-color: blue;"+"}";
	private String nodeStyle_closed = "node.closes {"+"fill-color: black;"+"}";
	private String nodeStyle_received = "node.received {"+"fill-color: red;"+"}";
	private String nodeStyle_territory = "node.territory {"+"fill-color: red;"+"}";
	private String nodeStyle=defaultNodeStyle+nodeStyle_agent+nodeStyle_open+nodeStyle_territory+nodeStyle_received+nodeStyle_closed;

	private Graph g; //data structure non serializable
	private Viewer viewer; //ref to the display,  non serializable
	private Integer nbEdges;//used to generate the edges ids

	private SerializableSimpleGraph<String, MapAttribute> sg;//used as a temporary dataStructure during migration
	
	public SerializableSimpleGraph<String, MapAttribute> getSg() {
		return this.sg;
	}
	
	public MapRepresentation(IExploBehaviour explo) {
		this.explo = explo;
		
		//System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);
		
		Platform.runLater(() -> {openGui();});//openGui();
		
		//this.viewer = this.g.display();

		this.nbEdges=0;
	}
	
	//TODO : coder méthodes manquantes et utiliser pour prendre décision de noeud à explorer si odeur détectée.
	/*
	public HashMap<String,Integer> getProba(HashMap<String,Integer> actualProbas){
		
		 //Retourne la liste des probas qu'un golem soit présent sur chaque noeud connu, en fonction des probas connues du tour précédent.
		 //Ces probas dépendent des noeuds odorants découverts. Ces probas sont indicatives et servent à prendre des décisions.
		 
		HashMap<String, Integer> newProbas = new HashMap<>();
		for( String entry : actualProbas.keySet()) {
		    int proba = actualProbas.get(entry);
			int nbVoisins = getNbVoisins(entry);//retourne le nombre de voisins
			for( String voisin : getConnexNodes(entry, true)) {//retourne les noeuds connexes, et le noeud entry si true
				if(newProbas.keySet().contains(voisin)) {
					int value = newProbas.get(voisin);
					value += proba / (nbVoisins+1);
					newProbas.replace(voisin, value);
				}
				else {
					int value = proba / (nbVoisins+1);
					newProbas.put(voisin, value);
				}
			}
		}
		return newProbas;
	}*/

	/**
	 * Add or replace a node and its attribute 
	 * @param id Id of the node
	 * @param mapAttribute associated state of the node
	 */
	public void addNode(String id,MapAttribute mapAttribute){
		//ADDED : si un noeud est envoyé comme ouvert alors qu'on l'avais visité et fermé, garder l'ancienne info ! 
		//ADDED : ajouter les noeuds ouverts à openNodes
		Node n;
		if (this.g.getNode(id)==null){
			n=this.g.addNode(id);
			n.clearAttributes();
			n.setAttribute("ui.class", mapAttribute.toString());
			n.setAttribute("ui.label",id);
			if(mapAttribute == MapAttribute.open) {//on prend en compte le nouvel attribut seulement si le noeud était ouvert
				this.explo.addOpenNode(id);//le noeud est ouvert
			}else {
				this.explo.removeOpenNode(id);//le noeud est fermé
			}
		}else{
			n=this.g.getNode(id);
			//un noeud fermé ne peut pas redevenir ouvert :
			if("open".equalsIgnoreCase((String)n.getAttribute("ui.class")) && mapAttribute.toString().equalsIgnoreCase("closed")) {//on prend en compte le nouvel attribut seulement si le noeud était ouvert
				n.clearAttributes();
				n.setAttribute("ui.class", "closed");
				n.setAttribute("ui.label",id);
				this.explo.removeOpenNode(id);//le noeud est fermé
			}	
		}
		
	}

	/**
	 * Add the edge if not already existing.
	 * @param idNode1 one side of the edge
	 * @param idNode2 the other side of the edge
	 */
	public void addEdge(String idNode1,String idNode2){
		try {
			this.nbEdges++;
			this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
		}catch (EdgeRejectedException e){
			//Do not add an already existing one
			//System.err.println("tryed to add existing edge");
			this.nbEdges--;
		}
	}
	
	/**
	 * Add to the graph the received informations (String)
	 * @param str, the string describing a graph or a part of graph serialised
	 */
	public void unserialize(String str) {
		if(str !=null) {
			System.out.println("Message Unserialize : "+str);
			String[] edges = str.split("-");
			for(String edge : edges) {
				String[] ids = edge.split(",");
				//System.out.println(ids.toString());
				String node1 = ids[1];
				String node2 = ids[3];
				MapAttribute att1 = ("closed".equalsIgnoreCase(ids[2]))?MapAttribute.closed:MapAttribute.open;
				MapAttribute att2 = ("closed".equalsIgnoreCase(ids[4]))?MapAttribute.closed:MapAttribute.open;
				addNode(node1,att1);
				addNode(node2,att2);
				addEdge(node1,node2);
			}
		}
	}

	/**
	 * return the map graph as a serialised String
	 */
	public String serialize() {
		String serialised = "";
		Iterator<Edge> iterE=this.g.edges().iterator();
		boolean isFirst = true;
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			if(!isFirst) {
				serialised+="-";
			}
			else {
				isFirst = false;
			}
			//System.out.println(sn.getAttribute("ui.class"));
			String att1 = (String) sn.getAttribute("ui.class");
			String att2 = (String) tn.getAttribute("ui.class");
			serialised+= e.getId()+","+sn.getId()+","+att1+","+tn.getId()+","+att2;
		}
		return serialised;
	}
	
	public String getNearestTargetAvailable(String idFrom, ArrayList<String> openNodes,ArrayList<String> agentsAround){
		/*
		 * Retourne le noeud ouvert qui n'est pas un obstacle le plus proche, 
		 * en évitant tout trajet comportant un obstacle
		 */
		ArrayList<String> nodes_copy = (ArrayList<String>) openNodes.clone();//copie des openNodes
		for(String obstacle : agentsAround) {//on retire des cibles potentielles les obstacles
			if(nodes_copy.contains(obstacle)) {
				nodes_copy.remove(obstacle);
			}
		}
		Iterator<Edge> iterE=this.g.edges().iterator();
		ArrayList<Edge> edgesToDelete = new ArrayList<>();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			for(String obstacle : agentsAround) {//on retire toutes les arêtes comportant un obstacle
				if((tn.toString().equalsIgnoreCase(obstacle)||sn.toString().equalsIgnoreCase(obstacle))) {
					edgesToDelete.add(e);
				}
			}
		}
		for(Edge toDelete : edgesToDelete) {
			g.removeEdge(toDelete);
		}
		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		String nearest = "";
		int nearest_distance = Integer.MAX_VALUE;//arbitrairement grand, il faudrait mettre infini mais bon...
		for(String node : nodes_copy) {//on regarde tous les noeuds ouverts
			Node n = g.getNode(node);
			if(n!=null) {
				Path p = dijkstra.getPath(n);
				if(p!=null) {
					List<Node> path=p.getNodePath(); //the shortest path from idFrom to idTo
					if(path!=null) {
						int pathLength = path.size();//taille du trajet
						if(pathLength < nearest_distance) {
							nearest = node;
							nearest_distance = pathLength;
						}
					}
				}
			}
		}
		for(Edge toDelete : edgesToDelete) {
			Node sn=toDelete.getSourceNode();
			Node tn=toDelete.getTargetNode();
			addEdge(sn.toString(), tn.toString());
		}
		System.out.println("Path :"+nearest+", "+nearest_distance);
		return nearest;
	}
	
	public ArrayList<String> getNearestTargetPathAvailable(String idFrom, ArrayList<String> openNodes,ArrayList<String> agentsAround){
		/*
		 * Retourne le trajet vers le noeud ouvert qui n'est pas un obstacle le plus proche, 
		 * en évitant tout trajet comportant un obstacle
		 */
		ArrayList<String> nodes_copy = (ArrayList<String>) openNodes.clone();//copie des openNodes
		for(String obstacle : agentsAround) {//on retire des cibles potentielles les obstacles
			if(nodes_copy.contains(obstacle)) {
				nodes_copy.remove(obstacle);
			}
		}
		Iterator<Edge> iterE=this.g.edges().iterator();
		ArrayList<Edge> edgesToDelete = new ArrayList<>();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			for(String obstacle : agentsAround) {//on retire toutes les arêtes comportant un obstacle
				if((tn.toString().equalsIgnoreCase(obstacle)||sn.toString().equalsIgnoreCase(obstacle))) {
					//System.out.println("remove edge : "+sn.toString()+","+tn.toString());
					if(!edgesToDelete.contains(e)) {
						edgesToDelete.add(e);
					}
					else {
						//System.out.println("edge already removed !");
					}
				}
			}
		}
		for(Edge toDelete : edgesToDelete) {
			g.removeEdge(toDelete);
		}
		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		String nearest = "";
		List<Node> nearest_path = null;
		int nearest_distance = Integer.MAX_VALUE;//arbitrairement grand, il faudrait mettre infini mais bon...
		for(String node : nodes_copy) {//on regarde tous les noeuds ouverts
			Node n = g.getNode(node);
			if(n!=null) {
				Path p = dijkstra.getPath(n);
				if(p!=null) {
					List<Node> path=p.getNodePath(); //the shortest path from idFrom to idTo
					if(path!=null) {
						int pathLength = path.size();//taille du trajet
						if(pathLength < nearest_distance) {
							nearest = node;
							nearest_distance = pathLength;
							nearest_path = path;
						}
					}
				}
			}
		}
		for(Edge toDelete : edgesToDelete) {
			Node sn=toDelete.getSourceNode();
			Node tn=toDelete.getTargetNode();
			addEdge(sn.toString(), tn.toString());
		}
		ArrayList<String> shortestPath=new ArrayList<String>();
		if(nearest_path!=null) {
			Iterator<Node> iter=nearest_path.iterator();
			if(iter!=null) {
				while (iter.hasNext()){
					shortestPath.add(iter.next().getId());
				}
				dijkstra.clear();
			}
		}
		System.out.println("Path to :"+nearest+", distance : "+nearest_distance);
		if(shortestPath.size()>0) {
			shortestPath.remove(0);//remove the current position
			return shortestPath;
		}
		else {
			return null;
		}
		
	}
	
	
	
	public String getTargetAvailableAtDistanceMax(String idFrom, ArrayList<String> openNodes,ArrayList<String> agentsAround, int distanceMax){
		/*
		 * Retourne le trajet vers le noeud ouvert qui n'est pas un obstacle le plus proche, 
		 * en évitant tout trajet comportant un obstacle
		 */
		ArrayList<String> nodes_copy = (ArrayList<String>) openNodes.clone();//copie des openNodes
		for(String obstacle : agentsAround) {//on retire des cibles potentielles les obstacles
			if(nodes_copy.contains(obstacle)) {
				nodes_copy.remove(obstacle);
			}
		}
		Iterator<Edge> iterE=this.g.edges().iterator();
		ArrayList<Edge> edgesToDelete = new ArrayList<>();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			for(String obstacle : agentsAround) {//on retire toutes les arêtes comportant un obstacle
				if((tn.toString().equalsIgnoreCase(obstacle)||sn.toString().equalsIgnoreCase(obstacle))) {
					//System.out.println("remove edge : "+sn.toString()+","+tn.toString());
					if(!edgesToDelete.contains(e)) {
						edgesToDelete.add(e);
					}
					else {
						//System.out.println("edge already removed !");
					}
				}
			}
		}
		for(Edge toDelete : edgesToDelete) {
			g.removeEdge(toDelete);
		}
		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		String nearest = "";
		int nearest_distance = Integer.MAX_VALUE;//arbitrairement grand, il faudrait mettre infini mais bon...
		for(String node : nodes_copy) {//on regarde tous les noeuds ouverts
			Node n = g.getNode(node);
			if(n!=null) {
				Path p = dijkstra.getPath(n);
				if(p!=null) {
					List<Node> path=p.getNodePath(); //the shortest path from idFrom to idTo
					if(path!=null) {
						int pathLength = path.size();//taille du trajet
						if(pathLength < nearest_distance && pathLength<=distanceMax+1 && pathLength>1) {
							nearest = node;
							nearest_distance = pathLength;
						}
					}
				}
			}
		}
		for(Edge toDelete : edgesToDelete) {
			Node sn=toDelete.getSourceNode();
			Node tn=toDelete.getTargetNode();
			addEdge(sn.toString(), tn.toString());
		}
		dijkstra.clear();
		System.out.println("Path to :"+nearest+", distance : "+nearest_distance);
		return nearest;
	}
	
	public int getEdgesCountAround(String node) {
		/*
		 * Retourne le nombre d'arêtes connectées à un noeud
		 * Si on ne connais pas le noeud dans le graphe (jamsi visité), retourne 0.
		 */
		Iterator<Edge> iterE=this.g.edges().iterator();
		int connected = 0;
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			if((tn.toString().equalsIgnoreCase(node)||sn.toString().equalsIgnoreCase(node))) {
				connected++;
			}
		}
		return connected;
	}
	
	public boolean isConnex(String node1,String node2) {
		/*
		 * Retourne le nombre d'arêtes connectées à un noeud
		 * Si on ne connais pas le noeud dans le graphe (jamsi visité), retourne 0.
		 */
		Iterator<Edge> iterE=this.g.edges().iterator();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			if(tn.toString().equalsIgnoreCase(node1)) {
				if(sn.toString().equalsIgnoreCase(node2)) {
					return true;
				}
			}
			if(sn.toString().equalsIgnoreCase(node1)) {
				if(tn.toString().equalsIgnoreCase(node2)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public ArrayList<String> getTargetPath(String idFrom, String goal ,ArrayList<String> agentsAround){
		/*
		 * Retourne le trajet vers le noeud ouvert qui n'est pas un obstacle le plus proche, 
		 * en évitant tout trajet comportant un obstacle
		 */
		Iterator<Edge> iterE=this.g.edges().iterator();
		ArrayList<Edge> edgesToDelete = new ArrayList<>();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			for(String obstacle : agentsAround) {//on retire toutes les arêtes comportant un obstacle
				if((tn.toString().equalsIgnoreCase(obstacle)||sn.toString().equalsIgnoreCase(obstacle))) {
					//System.out.println("remove edge : "+sn.toString()+","+tn.toString());
					if(!edgesToDelete.contains(e)) {
						edgesToDelete.add(e);
					}
					else {
						//System.out.println("edge already removed !");
					}
				}
			}
		}
		for(Edge toDelete : edgesToDelete) {
			g.removeEdge(toDelete);
		}
		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		List<Node> path = null;
		try {
			path=dijkstra.getPath(g.getNode(goal)).getNodePath();
		}
		catch(Exception e) {
			path = new ArrayList<>();
		}
		for(Edge toDelete : edgesToDelete) {
			Node sn=toDelete.getSourceNode();
			Node tn=toDelete.getTargetNode();
			addEdge(sn.toString(), tn.toString());
		}
		ArrayList<String> shortestPath=new ArrayList<String>();
		Iterator<Node> iter=path.iterator();
		while (iter.hasNext()){
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
		if(shortestPath.size()>0) {
			shortestPath.remove(0);//remove the current position
			return shortestPath;
		}
		else {
			return null;
		}
		
	}

	/**
	 * Compute the shortest Path from idFrom to IdTo. The computation is currently not very efficient
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo id of the destination node
	 * @return the list of nodes to follow
	 */
	public List<String> getShortestPath(String idFrom,String idTo){
		List<String> shortestPath=new ArrayList<String>();
		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		Node n = g.getNode(idTo);
		if(n!=null) {
			Path p = dijkstra.getPath(n);
			if(p!=null) {
				List<Node> path=p.getNodePath(); //the shortest path from idFrom to idTo
				if(path!=null) {
					Iterator<Node> iter=path.iterator();
					if(iter!=null) {
						while (iter.hasNext()){
							shortestPath.add(iter.next().getId());
						}
						dijkstra.clear();
					}
				}
			}
		}
		if(shortestPath.size()>0) {
			shortestPath.remove(0);//remove the current position
			return shortestPath;
		}
		else {
			return null;
		}
	}
	/**
	 * Compute new path taking account of obstacle
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo id of the destination node
	 * @return the list of nodes to follow
	 */
	public List<String> computeNewPath(String idFrom, String actual, String obstacle, List<String> openNodes){
		List<String> shortestPath=new ArrayList<String>();
		if(openNodes.contains(obstacle)) {
			openNodes.remove(obstacle);
		}
		Iterator<Edge> iterE=this.g.edges().iterator();
		boolean found = false;
		while (iterE.hasNext() && found == false){//on vérifie si l'arc qu'on veut retirer existe bien
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			if((tn.toString().equalsIgnoreCase(actual)&&sn.toString().equalsIgnoreCase(obstacle))
					|| (tn.toString().equalsIgnoreCase(obstacle)&&sn.toString().equalsIgnoreCase(actual))) {
				found = true;
			}
		}
		if(found) {
			g.removeEdge(actual, obstacle); //on retire l'arc vers l'obstacle
		}
		//Node obs = g.removeNode(obstacle); // on retire le noeud obstacle
		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		String nearest = "";
		int nearest_distance = Integer.MAX_VALUE;//arbitrairement grand, il faudrait mettre infini mais bon...
		for(String node : openNodes) {//on regarde tous les noeuds ouverts
			Node n = g.getNode(node);
			if(n!=null) {
				List<Node> path=dijkstra.getPath(n).getNodePath();
				if(path!=null) {
					int pathLength = path.size();//taille du trajet
					if(pathLength < nearest_distance) {
						nearest = node;
						nearest_distance = pathLength;
					}
				}
			}
		}
		System.out.println("Path :"+nearest+", "+nearest_distance);
		if(!nearest.equalsIgnoreCase("") && nearest_distance > 0) {
			List<Node> path=dijkstra.getPath(g.getNode(nearest)).getNodePath(); //the shortest path from idFrom to idTo
			Iterator<Node> iter=path.iterator();
			while (iter.hasNext()){
				shortestPath.add(iter.next().getId());
			}
			dijkstra.clear();
			shortestPath.remove(0);//remove the current position
		}
		if(found) {
			addEdge(actual, obstacle);//on remet l'arc vers l'obstacle
		}
		//Node n = g.addNode(obstacle); //on remet le noeud obstacle
		//n.clearAttributes();
		//n.setAttribute("ui.class", obs.getAttribute("ui.class"));
		//n.setAttribute("ui.label",obs.getAttribute("ui.label"));
		openNodes.add(obstacle);
		return shortestPath;
	}
	
	/**
	 * Find the nearest open node
	 */
	public int getPathLength(String idFrom, String idTo){
		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		String nearest = "";
		Node n = g.getNode(idTo);
		if(n!=null) {
			List<Node> path=dijkstra.getPath(n).getNodePath();
			if(path!=null) {
				int pathLength = path.size();//taille du trajet
				return pathLength-1;//distance au noeud idFrom
			}
		}
		return Integer.MAX_VALUE;
	}
	
	/**
	 * Find the nearest open node
	 */
	public String getNearestTarget(String idFrom, List<String> openNodes){
		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		String nearest = "";
		int nearest_distance = Integer.MAX_VALUE;//arbitrairement grand, il faudrait mettre infini mais bon...
		for(String node : openNodes) {//on regarde tous les noeuds ouverts
			List<Node> path=dijkstra.getPath(g.getNode(node)).getNodePath();
			if(path!=null) {
				int pathLength = path.size();//taille du trajet
				if(pathLength < nearest_distance) {
					nearest = node;
					nearest_distance = pathLength;
				}
			}
			/*int length_n = (int) dijkstra.getPathLength(g.getNode(node));//on calcule la longueur du chemin jusqu'à ce noeud ouvert
			if(length_n < nearest_distance) {
				nearest = node;
				nearest_distance = length_n;
			}*/
		}
		System.out.println("Path :"+nearest+", "+nearest_distance);
		return nearest;
	}
	
	/**
	 * Compute the shortest Path from idFrom to the nearest open node. The computation is currently not very efficient
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo id of the destination node
	 * @return the list of nodes to follow
	 */
	public List<String> getNearestTargetShortestPath(String idFrom, List<String> openNodes, String obstacle){
		List<String> shortestPath=new ArrayList<String>();
		boolean found = false;
		if(!obstacle.equalsIgnoreCase("")) {
			if(openNodes.contains(obstacle)) {
				openNodes.remove(obstacle);
			}
			Iterator<Edge> iterE=this.g.edges().iterator();
			while (iterE.hasNext() && found == false){//on vérifie si l'arc qu'on veut retirer existe bien
				Edge e=iterE.next();
				Node sn=e.getSourceNode();
				Node tn=e.getTargetNode();
				if((tn.toString().equalsIgnoreCase(idFrom)&&sn.toString().equalsIgnoreCase(obstacle))
						|| (tn.toString().equalsIgnoreCase(obstacle)&&sn.toString().equalsIgnoreCase(idFrom))) {
					found = true;
				}
			}
			if(found) {
				g.removeEdge(idFrom, obstacle); //on retire l'arc vers l'obstacle
			}
		}
		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		String nearest = "";
		int nearest_distance = Integer.MAX_VALUE;//arbitrairement grand, il faudrait mettre infini mais bon...
		for(String node : openNodes) {//on regarde tous les noeuds ouverts
			List<Node> path=dijkstra.getPath(g.getNode(node)).getNodePath();
			if(path!=null) {
				int pathLength = path.size();//taille du trajet
				if(pathLength < nearest_distance) {
					nearest = node;
					nearest_distance = pathLength;
				}
			}
			/*int length_n = (int) dijkstra.getPathLength(g.getNode(node));//on calcule la longueur du chemin jusqu'à ce noeud ouvert
			if(length_n < nearest_distance) {
				nearest = node;
				nearest_distance = length_n;
			}*/
		}
		System.out.println("Path :"+nearest+", "+nearest_distance);
		if(!nearest.equalsIgnoreCase("") && nearest_distance > 0) {
			List<Node> path=dijkstra.getPath(g.getNode(nearest)).getNodePath(); //the shortest path from idFrom to idTo
			Iterator<Node> iter=path.iterator();
			while (iter.hasNext()){
				shortestPath.add(iter.next().getId());
			}
			dijkstra.clear();
			shortestPath.remove(0);//remove the current position
		}
		if(!obstacle.equalsIgnoreCase("")) {
			if(found) {
				addEdge(idFrom, obstacle);//on remet l'arc vers l'obstacle
			}
			//Node n = g.addNode(obstacle); //on remet le noeud obstacle
			//n.clearAttributes();
			//n.setAttribute("ui.class", obs.getAttribute("ui.class"));
			//n.setAttribute("ui.label",obs.getAttribute("ui.label"));
			openNodes.add(obstacle);
		}
		shortestPath.remove(0);//remove the current position
		return shortestPath;
	}

	/**
	 * Before the migration we kill all non serializable components and store their data in a serializable form
	 */
	public void prepareMigration(){
		System.out.println("prepare migration");
		this.sg= new SerializableSimpleGraph<String,MapAttribute>();
		Iterator<Node> iter=this.g.iterator();
		while(iter.hasNext()){
			Node n=iter.next();
			sg.addNode(n.getId(),(MapAttribute)n.getAttribute("ui.class"));
		}
		Iterator<Edge> iterE=this.g.edges().iterator();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}

		closeGui();

		this.g=null;
		
		
	}
	
	public Graph getGraph() {
		return this.g;
	}

	/**
	 * After migration we load the serialized data and recreate the non serializable components (Gui,..)
	 */
	public void loadSavedData(){
		
		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);
		
		openGui();
		
		Integer nbEd=0;
		for (SerializableNode<String, MapAttribute> n: this.sg.getAllNodes()){
			this.g.addNode(n.getNodeId()).setAttribute("ui.class", n.getNodeContent().toString());
			for(String s:this.sg.getEdges(n.getNodeId())){
				this.g.addEdge(nbEd.toString(),n.getNodeId(),s);
				nbEd++;
			}
		}
		System.out.println("Loading done");
	}

	/**
	 * Method called before migration to kill all non serializable graphStream components
	 */
	private void closeGui() {
		//once the graph is saved, clear non serializable components
		if (this.viewer!=null){
			try{
				this.viewer.close();
			}catch(NullPointerException e){
				System.err.println("Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
			}
			this.viewer=null;
		}
	}

	/**
	 * Method called after a migration to reopen GUI components
	 */
	private void openGui() {
		this.viewer =new FxViewer(this.g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);////GRAPH_IN_GUI_THREAD);
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
		viewer.addDefaultView(true);
		g.display();
	}
}
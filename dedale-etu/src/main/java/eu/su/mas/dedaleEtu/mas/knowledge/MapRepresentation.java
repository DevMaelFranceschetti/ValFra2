package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javafx.application.Platform;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.CloseFramePolicy;

import dataStructures.serializableGraph.*;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploSoloBehaviour;

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
	private ExploSoloBehaviour explo;
	
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
	
	public MapRepresentation(ExploSoloBehaviour explo) {
		this.explo = explo;
		//System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);
		
		Platform.runLater(() -> {openGui();});//openGui();
		
		//this.viewer = this.g.display();

		this.nbEdges=0;
	}

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
			if(mapAttribute==MapAttribute.open) {//on prend en compte le nouvel attribut seulement si le noeud était ouvert
				this.explo.addOpenNode(id);//le noeud est ouvert
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
				MapAttribute att1 = "open".equalsIgnoreCase(ids[2])?MapAttribute.open:MapAttribute.closed;
				MapAttribute att2 = "open".equalsIgnoreCase(ids[4])?MapAttribute.open:MapAttribute.closed;
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
		List<Node> path=dijkstra.getPath(g.getNode(idTo)).getNodePath(); //the shortest path from idFrom to idTo
		Iterator<Node> iter=path.iterator();
		while (iter.hasNext()){
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
		shortestPath.remove(0);//remove the current position
		return shortestPath;
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
		while (iterE.hasNext() && found == false){
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
		double nearest_distance = 10000000000000000.0;//arbitrairement grand, il faudrait mettre infini mais bon...
		for(String node : openNodes) {//on regarde tous les noeuds ouverts
			double length_n = dijkstra.getPathLength(g.getNode(node));//on calcule la longueur du chemin jusqu'à ce noeud ouvert
			if(length_n < nearest_distance) {
				nearest = node;
				nearest_distance = length_n;
			}
		}
		List<Node> path=dijkstra.getPath(g.getNode(nearest)).getNodePath(); //the shortest path from idFrom to idTo
		Iterator<Node> iter=path.iterator();
		while (iter.hasNext()){
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
		shortestPath.remove(0);//remove the current position
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
	 * Compute the shortest Path from idFrom to the nearest open node. The computation is currently not very efficient
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo id of the destination node
	 * @return the list of nodes to follow
	 */
	public List<String> getNearestTargetShortestPath(String idFrom, List<String> openNodes){
		List<String> shortestPath=new ArrayList<String>();

		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		String nearest = "";
		double nearest_distance = 10000000000000000.0;//arbitrairement grand, il faudrait mettre infini mais bon...
		for(String node : openNodes) {//on regarde tous les noeuds ouverts
			double length_n = dijkstra.getPathLength(g.getNode(node));//on calcule la longueur du chemin jusqu'à ce noeud ouvert
			if(length_n < nearest_distance) {
				nearest = node;
				nearest_distance = length_n;
			}
		}
		List<Node> path=dijkstra.getPath(g.getNode(nearest)).getNodePath(); //the shortest path from idFrom to idTo
		Iterator<Node> iter=path.iterator();
		while (iter.hasNext()){
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
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
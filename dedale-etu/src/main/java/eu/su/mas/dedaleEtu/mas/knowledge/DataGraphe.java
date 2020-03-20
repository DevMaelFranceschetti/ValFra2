package eu.su.mas.dedaleEtu.mas.knowledge;

import java.util.Iterator;

import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;

public class DataGraphe {
	
	/*
	 * Simplified method to serialise a graphe into a String with reduced length
	 * Return all the connexions of the graph
	 */
	public static String serialize(Graph graphe) {
		String serialised = "";
		Iterator<Edge> iterE=graphe.edges().iterator();
		boolean isFirst = true;
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			//MapAttribute typeSn = (MapAttribute)sn.getAttribute("ui.class");
			//MapAttribute typeTn = (MapAttribute)tn.getAttribute("ui.class");
			if(!isFirst) {
				serialised+="-";
			}
			else {
				isFirst = false;
			}
			serialised+= e.getId()+","+sn.getId()+","+tn.getId();
		}
		return serialised;
	}
	
	public static Graph unserializeTo(String str,Graph graph) {
		
		String[] edges = str.split("-");
		Integer nbEdges = graph.getEdgeCount();
		for(String edge : edges) {
			String[] ids = edge.split(",");
			//ajout des noeuds si pas déjà existants:
			if (graph.getNode(ids[1])==null){
				graph.addNode(ids[1]);
			}
			if (graph.getNode(ids[2])==null){
				graph.addNode(ids[2]);
			}
			//ajout de l'arête :
			try {
				nbEdges++;
				graph.addEdge(nbEdges.toString(),ids[1], ids[2]);
			}catch (EdgeRejectedException e){
				//Do not add an already existing one
				nbEdges--;
			}
		}
		return graph;
	}
	
public static Graph unserialize(String str) {
		Graph graph = new SingleGraph("New graph");
		String[] edges = str.split("-");
		Integer nbEdges = 0;
		for(String edge : edges) {
			String[] ids = edge.split(",");
			System.out.println(ids);
			graph.addNode(ids[1]);
			graph.addNode(ids[2]);
			//ajout de l'arête :
			try {
				nbEdges++;
				graph.addEdge(nbEdges.toString(),ids[1], ids[2]);
			}catch (EdgeRejectedException e){
				//Do not add an already existing one
				nbEdges--;
			}
		}
		return graph;
	}

}

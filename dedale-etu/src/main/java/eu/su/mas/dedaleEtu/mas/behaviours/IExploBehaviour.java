package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

public interface IExploBehaviour {
	public String getNextNode();
	
	public MapRepresentation getMap();

	public List<String> getLastMoves();
	
	public void addOpenNode(String node);
	
	public void removeOpenNode(String node);
	
	public ArrayList<String> getOpenNodes();
}

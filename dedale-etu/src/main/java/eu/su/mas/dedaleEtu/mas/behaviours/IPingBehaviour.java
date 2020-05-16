package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.List;

import jade.core.AID;

public interface IPingBehaviour {

	public ArrayList<String> getAgentsAround();
	
	public ArrayList<AID> getTargetList();
	
	public boolean needWait();
	
	public boolean isBoss();
	
	public String getBossPosition();
	
	public ArrayList<String> getCoalitionAgents();
	
	public boolean hasCoalition();
	
	public boolean needReturnBoss();
	
	public void informBossMoving(String dest);
	
	public boolean needInformBossLefted();
	
	public String getOldBossPos();
	
	public String getOldBossName();
}

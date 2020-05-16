package eu.su.mas.dedaleEtu.mas.strategy;

import java.io.Serializable;
import java.util.ArrayList;

public class BossStrategy implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static int distanceMove = 1;//paramètre
	private static int maxTime = 5;//paramètre
	private int counter = 0;
	private ArrayList<String> coalitionMembers = new ArrayList<>();
	private ArrayList<String> informed = new ArrayList<>();
	
	public void setCoalitionMembers(ArrayList<String> members) {
		this.coalitionMembers = (ArrayList<String>)members.clone();
	}
	
	public void moved() {
		this.counter = 0;
		this.informed = new ArrayList<>();
	}
	
	public void incrementTime() {
		this.counter +=1;
	}
	
	public boolean needToMove() {
		return this.counter>maxTime;
	}
	
	public void informed(String name) {
		//tout agent informé et renvoyant confirmation fait nécessairement partie de la coalition
		if(!this.coalitionMembers.contains(name)) {
			this.coalitionMembers.add(name); 
		}
		if(!this.informed.contains(name)) {
			this.informed.add(name);
		}
		System.out.println("Coalition members : "+String.join(",",this.coalitionMembers)+", informed :"+String.join(",", this.informed));
	}
	
	public boolean allInformed() {
		return this.informed.size()>=this.coalitionMembers.size();
	}
	
}

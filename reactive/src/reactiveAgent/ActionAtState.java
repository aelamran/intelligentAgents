package reactiveAgent;

import logist.topology.Topology.City;

public class ActionAtState {
	
	private boolean decision;
	private City nextCity;
	private int id;
	
	
	public ActionAtState(boolean decision, City nextCity, int id) {
		// why call super ?
		super();
		this.decision = decision;
		this.nextCity = nextCity;
		this.id = id;
	}
	
	
	public boolean getDecision() {
		return decision;
	}
	public void setDecision(boolean decision) {
		this.decision = decision;
	}
	public City getNextCity() {
		return nextCity;
	}
	public void setNextCity(City nextCity) {
		this.nextCity = nextCity;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	

}

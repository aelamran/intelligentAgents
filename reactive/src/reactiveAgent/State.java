package reactiveAgent;

import logist.topology.Topology.City;
import logist.task.Task;


public class State {
	
	private City currentCity;
	private Task currentTask;
	private int id;
	
	public State(City currentCity, Task currentTask, int id) {
		super();
		this.currentCity = currentCity;
		this.currentTask = currentTask;
		this.id = id;
	}

	
	public City getCurrentCity() {
		return currentCity;
	}

	public void setCurrentCity(City currentCity) {
		this.currentCity = currentCity;
	}

	public Task getCurrentTask() {
		return currentTask;
	}

	public void setCurrentTask(Task currentTask) {
		this.currentTask = currentTask;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	
	
	

}

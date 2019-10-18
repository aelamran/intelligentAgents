package deliberativeAgent;

import logist.topology.Topology.City;

import java.util.List;

import logist.task.Task;
import logist.task.TaskSet;


public class State {
	
	private TaskSet currentTasks;
	private TaskSet availableTasks;	
	private City currentCity;
	private double cost=0;
	
	//private Task currentTask;
	private int id;
	
	public State(TaskSet carriedTasks, TaskSet availableTasks, City currentCity, int id) {
		super();
		this.currentTasks = carriedTasks;
		this.availableTasks = availableTasks;
		this.currentCity = currentCity;
		this.id = id;
	}

	
	public TaskSet getCurrentTasks() {
		return currentTasks;
	}


	public TaskSet getAvailableTasks() {
		return availableTasks;
	}


	public City getCurrentCity() {
		return currentCity;
	}

	public void setCurrentCity(City currentCity) {
		this.currentCity = currentCity;
	}


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}


	public double getCost() {
		return cost;
	}


	public void setCost(double cost) {
		this.cost = cost;
	}
	
}

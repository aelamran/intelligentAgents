package deliberativeAgent;
import logist.task.TaskSet;
import logist.topology.Topology.City;


public class State {
	
	private TaskSet currentTasks;
	private TaskSet availableTasks;	
	private City currentCity;

	//private Task currentTask;
	private double cost=0;
	private int id;
	private double heuristic=0;
	
	public State(TaskSet currentTasks, TaskSet availableTasks, City currentCity, int id) {
		super();
		this.currentTasks = currentTasks;
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
	
	public boolean isGoalState() {
		if (currentTasks.isEmpty() && availableTasks.isEmpty()) {
			return true;
		}
		else {
			return false;
		}
	}
		


	public double getCost() {
		return cost;
	}

	public double getHeuristic() {
		return heuristic;
	}


	public void setCost(double cost) {
		this.cost = cost;
	}

	public void setHeuristic(double heuristic) {
		this.heuristic = heuristic;
	}
	
}

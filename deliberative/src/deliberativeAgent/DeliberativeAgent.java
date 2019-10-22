package deliberativeAgent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Action.Delivery;
import logist.plan.Action.Pickup;
import logist.plan.Plan;
/* import table */
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;


/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeAgent implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			plan = aStarPlan(vehicle, tasks);
			break;
		case BFS:
			plan = bfsPlan(vehicle, tasks);	
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		
		return plan;
	}
	
	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {
		long startTime = System.nanoTime();
		HashMap <Integer, State> parentStates = new HashMap<Integer, State>();
		HashMap <Integer, Action> parentAction = new HashMap<Integer, Action>();
		ArrayList<Action> finalActions = new ArrayList<Action>();
		ArrayList<City> bestCitiesPath = new ArrayList<City>();
		HashSet<State> visitedState = new HashSet<State>();
		LinkedList<State> queue = new LinkedList<State>();	
	
		Plan plan = new Plan(vehicle.getCurrentCity());
		int i =0;
		State initialState = new State(vehicle.getCurrentTasks(), tasks, vehicle.getCurrentCity(), i);
		i++;
		double tmpCost = 0.0;
		double finalCost = Double.MAX_VALUE;
		visitedState.add(initialState);
		queue.add(initialState);
		
		while(queue.size() != 0)
		{
			initialState = queue.poll();				
			if(initialState.isGoalState()){
				tmpCost = 0.0;
				ArrayList<Action> path = new ArrayList<Action>();
				ArrayList<City> cities = new ArrayList<City>();
				
				State parentState = initialState;
				State interState = initialState;
				while(parentStates.containsKey(parentState.getId())){
					Action toAdd = parentAction.get(parentState.getId());
					path.add(0, toAdd);
					cities.add(0, parentState.getCurrentCity());
					parentState = parentStates.get(parentState.getId());
					tmpCost += vehicle.costPerKm()*interState.getCurrentCity().distanceTo(parentState.getCurrentCity());
					interState = parentState;
				}
				
				if(tmpCost<=finalCost) {
					finalCost = tmpCost;
					finalActions = path;
					bestCitiesPath = cities;
				}
				
			}
			
			List<State> neighboursState = new ArrayList<State>();
			for (Task task: initialState.getCurrentTasks()) {
				TaskSet carriedTasks = initialState.getCurrentTasks().clone();
				carriedTasks.remove(task);
				State tmpState = new State(carriedTasks, initialState.getAvailableTasks(), task.deliveryCity, i);
				tmpState.setCost(tmpState.getCost()+initialState.getCurrentCity().distanceTo(tmpState.getCurrentCity()));
				neighboursState.add(tmpState);
				parentStates.put(i, initialState);
				parentAction.put(i, new Delivery(task));
				i++;	
			}
			
			for(Task task: initialState.getAvailableTasks()) {
				if(vehicle.capacity() >= initialState.getCurrentTasks().weightSum()+ task.weight) {
					TaskSet carriedTasks = initialState.getCurrentTasks().clone();
					TaskSet availableTasks = initialState.getAvailableTasks().clone();				
					availableTasks.remove(task);
					carriedTasks.add(task);
					State tmpState = new State(carriedTasks, availableTasks, task.pickupCity, i);
					
					tmpState.setCost(tmpState.getCost()+initialState.getCurrentCity().distanceTo(tmpState.getCurrentCity()));
					neighboursState.add(tmpState);
					parentStates.put(i, initialState);
					parentAction.put(i, new Pickup(task));
					i++;
				}
			}
		
			for(State state: neighboursState) {
				if(isFinalState(state)) {
					queue.add(state);
				}
				if(!(isFinalState(state)) && !visitedState(visitedState, state)) {
					
					visitedState.add(state);
					queue.add(state);
				}
			}
		}		

		return getPlan(vehicle, finalActions, bestCitiesPath);
	}

	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {
		HashMap<Integer, Double> visitedStatesHashMap = new HashMap<Integer, Double>();
		List<State> visitedStates = new ArrayList<State>();
		HashMap<Integer, State> parentState = new HashMap<Integer, State>();
		HashMap<Integer, Action> previousAction = new HashMap<Integer, Action>();
		ArrayList<Action> finalActions = new ArrayList<Action>();
		ArrayList<City> finalCitiesPath = new ArrayList<Topology.City>();
		ArrayList<Action> pathActions = new ArrayList<Action>();
		PriorityQueue<State> Q = new PriorityQueue<State>(new Comparator<State>() {
			@Override
			public int compare(State state1, State state2) {
				return Double.compare(state1.getCost() + state1.getHeuristic(), state2.getCost()+state2.getHeuristic());
			}
		});

        long startTime = System.nanoTime();
		int costPerKm = vehicle.costPerKm();
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		int i = 0;
		TaskSet vehicleCurrentTasks = vehicle.getCurrentTasks();
		State initialState = new State(vehicleCurrentTasks, tasks, vehicle.getCurrentCity(), i);
		i++;
		initialState.setCost(0.0);
		Q.add(initialState);
		
		State headState = initialState;
		List<State> successors;
		while (!Q.isEmpty()) {
			successors = new ArrayList<State>();
			headState = Q.poll();
			if (isFinalState(headState)) {
				break;
			}
			if (!visitedState(visitedStates, headState)) {	
				visitedStates.add(headState);
				
				for (Task task : headState.getCurrentTasks()) {
					TaskSet currentTasks = headState.getCurrentTasks().clone();
					currentTasks.remove(task);

					State newState = new State(currentTasks, headState.getAvailableTasks(), task.deliveryCity, i);
					newState.setCost(headState.getCost() + headState.getCurrentCity().distanceTo(task.deliveryCity));
					newState.setHeuristic(computeHeuristic(newState, costPerKm));
					successors.add(newState);
					parentState.put(i, headState);
					previousAction.put(i, new Delivery(task));
					i++;
				}

				for (Task task : headState.getAvailableTasks()) {
					if (task.weight < (vehicle.capacity() - headState.getCurrentTasks().weightSum())) {
						TaskSet availableTasks = headState.getAvailableTasks().clone();
						availableTasks.remove(task);
						TaskSet currentTasks = headState.getCurrentTasks().clone();
						currentTasks.add(task);
						
						State newState = new State(currentTasks, availableTasks, task.pickupCity, i);
						newState.setCost(headState.getCost() + headState.getCurrentCity().distanceTo(task.pickupCity));
						newState.setHeuristic(computeHeuristic(newState, costPerKm));
						successors.add(newState);
						parentState.put(i, headState);
						previousAction.put(i, new Pickup(task));
						i++;
					}
				}

				Q.addAll(successors);
			}
		}


		City oldCity = current;
		while(parentState.containsKey(headState.getId()) ){
			Action action = previousAction.get(headState.getId());
			finalActions.add(action);
			finalCitiesPath.add(headState.getCurrentCity());
			headState = parentState.get(headState.getId());	
	
		}
		for (int j = finalActions.size()-1; j >= 0; j--) {
			Action action = finalActions.get(j);
			for (City city : oldCity.pathTo(finalCitiesPath.get(j))) {
				plan.appendMove(city);
			}
			oldCity = finalCitiesPath.get(j);
			plan.append(action);
			System.out.println(action);
		}
		return plan;
		
	}

	private double computeHeuristic(State newState, int costPerKm) { 
		// Method computing our heuristic which is the  tight highest cost of all the remaining tasks
		Double heuristic;
		Double mini = Double.MAX_VALUE;
		Double maxi = 0.0;
		City currentCity = newState.getCurrentCity();
		for (Task task : newState.getCurrentTasks()) {
			Double dist = currentCity.distanceTo(task.deliveryCity);
			if (mini > dist){
				mini = dist;
			}
			if (maxi < dist){
				maxi = dist;
			}
		}
		for (Task task : newState.getAvailableTasks()) {
			Double dist = task.pickupCity.distanceTo(task.deliveryCity);
			if (maxi < dist){
				maxi = dist;
			}			
		}
		return maxi*costPerKm;		
	}

	private boolean isFinalState(State state) {
		return state.getAvailableTasks().isEmpty() && state.getCurrentTasks().isEmpty();
	}

	public boolean stateEqualsState(State state, State currentState) {
	if(state.getAvailableTasks().equals(currentState.getAvailableTasks()) &&
			state.getCurrentCity().equals(currentState.getCurrentCity()) &&
			state.getCurrentTasks().equals(currentState.getCurrentTasks()) &&
			(state.getCost() + state.getHeuristic() < currentState.getCost() + currentState.getHeuristic())){
				return true;
	}
	return false;
	}
	public boolean visitedState(Collection<State> visited, State currentState){	
		for(State state:visited) {		
				if (stateEqualsState(state, currentState)) {
					return true;
				}
		}
		return false;	
	}

	public Plan getPlan(Vehicle vehicle, ArrayList<Action> bestPath, ArrayList<City> finalCitiesPath){
		City oldCity = vehicle.getCurrentCity();
		Plan plan = new Plan(oldCity);
		for (int j=0; j<bestPath.size(); j++) {
			Action action = bestPath.get(j);
			for (City city : oldCity.pathTo(finalCitiesPath.get(j))) {
				plan.appendMove(city);
			}
			oldCity = finalCitiesPath.get(j);
			plan.append(action);
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		System.out.println("plan cancelled");
		if (!carriedTasks.isEmpty()) {
			// The plan computation already takes care of this by taking into consideration
			// the tasks already carried
		}
	}
}

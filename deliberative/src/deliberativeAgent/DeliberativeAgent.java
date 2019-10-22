package deliberativeAgent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

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
	
	public boolean stateEqualsState(State state, State currentState) {
		if(state.getAvailableTasks().equals(currentState.getAvailableTasks()) &&
				state.getCurrentCity().equals(currentState.getCurrentCity()) &&
				state.getCurrentTasks().equals(currentState.getCurrentTasks()) &&
				state.getCost() < currentState.getCost()){
					return true;
		}
		return false;
	}

	public boolean visitedState(List<State> visited, State currentState){	
		for(State state:visited) {		
				if (stateEqualsState(state, currentState)) {
					return true;
				}
		}
		return false;	
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
		Plan plan = new Plan(vehicle.getCurrentCity());
		int i =0;
		State initialState = new State(vehicle.getCurrentTasks(), tasks, vehicle.getCurrentCity(), i);
		
		i++;
		
		List<State> visitedState = new LinkedList<State>();
		LinkedList<State> queue = new LinkedList<State>();
		
		HashMap <Integer, State> parentStates = new HashMap<Integer, State>();
		HashMap <Integer, Action> parentAction = new HashMap<Integer, Action>();
		LinkedList<Action> bestPath = new LinkedList<Action>();
		LinkedList<City> bestCitiesPath = new LinkedList<City>();
		double tmpCost = 0.0;
		double finalCost = Double.MAX_VALUE;
		visitedState.add(initialState);
		queue.add(initialState);
		int numbGoals = 0;
		
		while(queue.size() != 0)
		{
			initialState = queue.poll();				
			if(initialState.isGoalState()){
				numbGoals++;
				tmpCost = 0.0;
				LinkedList<Action> path = new LinkedList<Action>();
				LinkedList<City> cities = new LinkedList<City>();
				
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
					bestPath = path;
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
		
		City oldCity = vehicle.getCurrentCity();
		plan = new Plan(oldCity);
		for (int j=0; j<bestPath.size(); j++) {
			Action action = bestPath.get(j);
			for (City city : oldCity.pathTo(bestCitiesPath.get(j))) {
				plan.appendMove(city);
			}
			oldCity = bestCitiesPath.get(j);
			plan.append(action);
			
		}
		System.out.println((System.nanoTime() - startTime)/1000000000.0 + " seconds have passed for BFS");
		System.out.println("total distance bfs "+plan.totalDistance());
		System.out.println(plan);
		return plan;
	}

	private boolean visitedAndHigherCost(List<State> visited, State currentState) {
		for (State state : visited) {
			if (stateEqualsState(state, currentState)) {
				if (state.getCost() > currentState.getCost()) {
					return true;
				}
			}
		}
		return false;
	}

	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {
        long startTime = System.nanoTime();

		int costPerKm = vehicle.costPerKm();
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		PriorityQueue<State> Q = new PriorityQueue<State>(new Comparator<State>() {
			@Override
			public int compare(State state1, State state2) {
				return Double.compare(state1.getCost(), state2.getCost());
			}
		});
		int i = 0;

		TaskSet vehicleCurrentTasks = vehicle.getCurrentTasks();

		State initialState = new State(vehicleCurrentTasks, tasks, vehicle.getCurrentCity(), i);
		i++;
		initialState.setCost(0.0);
		Q.add(initialState);
		// We store states in visitedStates
		HashMap<Integer, Double> visitedStatesHashMap = new HashMap<Integer, Double>();
		List<State> visitedStates = new ArrayList<State>();

		HashMap<Integer, State> parentState = new HashMap<Integer, State>();
		HashMap<Integer, Action> previousAction = new HashMap<Integer, Action>();

		State headState = initialState;
		List<State> successors;
		while (!Q.isEmpty()) {
			successors = new ArrayList<State>();
			headState = Q.poll();
			if (isFinalState(headState)) {
				break;
			}
			if (!visitedState(visitedStates, headState) ||  visitedAndHigherCost(visitedStates, headState)) {	
				visitedStates.add(headState);
				
				for (Task task : headState.getCurrentTasks()) {
					TaskSet currentTasks = headState.getCurrentTasks().clone();
					currentTasks.remove(task);
					State newState = new State(currentTasks, headState.getAvailableTasks(), task.deliveryCity, i);
					
					newState.setCost(headState.getCost()+ getCost(headState, newState, costPerKm));
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
						
						newState.setCost(headState.getCost()+getCost(headState, newState, costPerKm));
						successors.add(newState);
						
						parentState.put(i, headState);
						previousAction.put(i, new Pickup(task));
						
						i++;
					}
				}

				Q.addAll(successors);
			}
		}
		ArrayList<Action> finalActions = new ArrayList<Action>();
		ArrayList<City> citiesPath = new ArrayList<Topology.City>();
		
		ArrayList<Action> pathActions = new ArrayList<Action>();

		City oldCity = current;

		while(parentState.containsKey(headState.getId()) ){
			Action action = previousAction.get(headState.getId());
			finalActions.add(action);
			citiesPath.add(headState.getCurrentCity());
			headState = parentState.get(headState.getId());	
	
		}
		for (int j = finalActions.size()-1; j >= 0; j--) {
			Action action = finalActions.get(j);
			for (City city : oldCity.pathTo(citiesPath.get(j))) {
				plan.appendMove(city);
			}
			oldCity = citiesPath.get(j);
			plan.append(action);
			System.out.println(action);
		}
		System.out.println("total distance astar "+plan.totalDistance());
		Double totalTime = (System.nanoTime() - startTime)/1000000000.0 ;
		
		System.out.println(totalTime+ " seconds have passed for A*");
		return plan;

	}

	private double getCost(State headState, State newState, int costPerKm) {
		// TODO Auto-generated method stub
		Double heuristic = 0.0;
		int i = 0;
		City currentCity = newState.getCurrentCity();
		for (Task task : newState.getCurrentTasks()) {
			heuristic += currentCity.distanceTo(task.deliveryCity);
			i++;
		}
		for (Task task : newState.getAvailableTasks()) {
			heuristic += currentCity.distanceTo(task.pickupCity);
			heuristic += currentCity.distanceTo(task.deliveryCity);
			i++;
		}
		
		return headState.getCurrentCity().distanceTo(newState.getCurrentCity()) * costPerKm + heuristic*costPerKm;
	}

	private double getCost4(State headState, State newState, int costPerKm) {
		// TODO Auto-generated method stub
		Double heuristic = 0.0;
		City currentCity = newState.getCurrentCity();
		for (Task task : newState.getCurrentTasks()) {
			heuristic -= task.reward;
			currentCity = task.deliveryCity;
		}
		for (Task task : newState.getAvailableTasks()) {
			heuristic -= task.reward;
			currentCity = task.deliveryCity;
		}
		//System.out.println(heuristic);
		//heuristic = newState.getCurrentCity().distanceTo(currentCity)*costPerKm;
		return headState.getCurrentCity().distanceTo(newState.getCurrentCity()) * costPerKm + heuristic;
	}
//old
	private double getCost7(State headState, State newState, int costPerKm) {
		// TODO Auto-generated method stub
		Double heuristic = 0.0;
		int i = 0;
		City currentCity = newState.getCurrentCity();
		for (Task task : newState.getCurrentTasks()) {
			heuristic += currentCity.distanceTo(task.deliveryCity);
			currentCity = task.deliveryCity;
			i++;
		}
		for (Task task : newState.getAvailableTasks()) {
			heuristic += currentCity.distanceTo(task.pickupCity);
			currentCity = task.pickupCity;
			heuristic += currentCity.distanceTo(task.deliveryCity);
			currentCity = task.deliveryCity;
			i++;
		}
		
		//heuristic/= i;
		//System.out.println(heuristic);
		//heuristic = newState.getCurrentCity().distanceTo(currentCity)*costPerKm;
		return headState.getCurrentCity().distanceTo(newState.getCurrentCity()) * costPerKm + heuristic*costPerKm;
	}

	private boolean isFinalState(State state) {
		return state.getAvailableTasks().isEmpty() && state.getCurrentTasks().isEmpty();
	}

	private int cost(State state) {
		return 0;
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
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

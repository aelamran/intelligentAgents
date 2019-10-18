package deliberativeAgent;

/* import table */
import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Action.Pickup;
import logist.plan.Action.Move;
import logist.plan.Action.Delivery;
import logist.plan.Plan;
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

	enum Algorithm {
		BFS, ASTAR
	}

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
			// ...
			plan = aStarPlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		return plan;
	}

	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		PriorityQueue<State> Q = new PriorityQueue<State>(new Comparator<State>() {
			@Override
			public int compare(State state1, State state2) {
				return Double.compare(state1.getCost(), state2.getCost());
				// return Integer.compare(node0.getF(), node1.getF());
			}
		});
		int i = 0;
		State initialState = new State(vehicle.getCurrentTasks(), tasks, vehicle.getCurrentCity(), i);
		i++;
		initialState.setCost(0.0);
		Q.add(initialState);
		// We store states ids and costs in C
		HashMap<Integer, Double> C = new HashMap<Integer, Double>();

		HashMap<Integer, State> parentState = new HashMap<Integer, State>();
		HashMap<Integer, Action> previousAction = new HashMap<Integer, Action>();

		State headState = initialState;
		List<State> successors;
		while (!Q.isEmpty()) {
			////System.out.println("Q not empty");
			successors = new ArrayList<State>();
			headState = Q.poll();
			if (isFinalState(headState)) {
				break;
			}
			if ((!C.containsKey(headState.getId())) || (C.get(headState.getId()) > headState.getCost())) {
				C.put(headState.getId(), headState.getCost());
				
				////System.out.println(headState.getAvailableTasks().size());
				for (Task task : headState.getCurrentTasks()) {
					TaskSet currentTasks = headState.getCurrentTasks().clone();
					currentTasks.remove(task);
					State newState = new State(currentTasks, headState.getAvailableTasks(), task.deliveryCity, i);
					
					newState.setCost(getCost(headState, newState));
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
						
						newState.setCost(getCost(headState, newState));
						successors.add(newState);
						
						parentState.put(i, headState);
						previousAction.put(i, new Pickup(task));
						
						i++;
					}
				}
				successors.sort(new Comparator<State>() {
					@Override
					public int compare(State state1, State state2) {
						return Double.compare(state1.getCost(), state2.getCost());
						// return Integer.compare(node0.getF(), node1.getF());
					}
				});
				Q.addAll(successors);
			}
		}
		ArrayList<Action> finalActions = new ArrayList<Action>();
		ArrayList<City> citiesPath = new ArrayList<Topology.City>();
		
		ArrayList<Action> pathActions = new ArrayList<Action>();

		City oldCity = current;

		while(parentState.containsKey(headState.getId()) ){
			////System.out.println(headState.getId());
			Action action = previousAction.get(headState.getId());
			finalActions.add(action);
			citiesPath.add(headState.getCurrentCity());
			headState = parentState.get(headState.getId());	
			
			/*for (City city : oldCity.pathTo(headState.getCurrentCity())) {
				pathActions.add(0, new Move(city));
			}
			pathActions.add(0, action);
			oldCity = headState.getCurrentCity();*/
		}
		//plan = new Plan(current, pathActions);
		for (int j = finalActions.size()-1; j >= 0; j--) {
			Action action = finalActions.get(j);
			//plan.append(new Move(citiesPath.get(j)));
			for (City city : oldCity.pathTo(citiesPath.get(j))) {
				//System.out.println(city);
				plan.appendMove(city);
			}
			oldCity = citiesPath.get(j);
			//System.out.println(finalActions.get(j).toString());
			plan.append(action);
		}
		
		return plan;

	}

	private double getCost(State headState, State newState) {
		// TODO Auto-generated method stub
		
		return headState.getCurrentCity().distanceTo(newState.getCurrentCity());
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
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}

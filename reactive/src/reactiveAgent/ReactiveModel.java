package reactiveAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveModel implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private ArrayList<State> myStates;
	private ArrayList<ActionAtState> myActions;
	
	private HashMap<Integer,HashMap<Integer,Double>> myRewards;

	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		

		
		// Create and stock the states
		myStates = new ArrayList<State>();
		State state;
		Task task;
		int i = 0;
		for (City city1 : topology) {
			for (City city2 : topology) {
				task = new Task(i, city1, city2, td.reward(city1, city2), td.weight(city1, city2));
				myStates.add(new State(city1, task, i));
				i++;
			}
		}

		// Create the actions
		myActions = new ArrayList<ActionAtState>();
		int id = 0;
		
		for (City city: topology) {
		// Add an action for each city and decision
			myActions.add(new ActionAtState(true, city, id));
			id++;
			
			myActions.add(new ActionAtState(false, city, id));
			id++;
			
		}
		/*for (State myState : myStates) {
			// Add the action of accepting the task
			myActions.add(new ActionAtState(true, state.getCurrentTask().deliveryCity, id));
			id++;
			
			// Add the actions of refusing the task and moving to a neighboring city
			City myCity = myState.getCurrentCity();
			for (City neighbor : myCity) {
				myActions.add(new ActionAtState(false, neighbor, id));
				id++;
			}
		}*/
		
		myRewards = new HashMap<Integer, HashMap<Integer,Double>>();
		HashMap<Integer,Double> stateRewards;
		int state_id;
		int action_id;		
		City source;
		City destination;

		// Get the cost per km
		double costPerKm = agent.vehicles().get(0).costPerKm();
		
		
		// Build the rewards
		for (State myState : myStates) {
			source = myState.getCurrentTask().pickupCity;
			destination = myState.getCurrentTask().deliveryCity;
			state_id = myState.getId();
			stateRewards = new HashMap<Integer, Double>();
			for (ActionAtState action : myActions) {
				if (action.getNextCity().id == destination.id) {
					action_id = action.getId();
					if (action.getDecision()) {
						// If we accept the task
						stateRewards.put(action_id, td.reward(source, destination) - costPerKm * source.distanceTo(destination));
					}
					else {
						// If we refuse the task
						stateRewards.put(action_id, -costPerKm * source.distanceTo(destination));
					}
				}
			}
			myRewards.put(state_id, stateRewards);
		}
		
		//TODO : Build the transitions, Best, and V
		

		
		
		
	}

		
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
	}

}

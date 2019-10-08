package reactiveAgent;

import java.util.ArrayList;
import java.util.Arrays;
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
	private ActionAtState[] best_actions;
	private Double[] valueTable;

	private HashMap<Integer, HashMap<Integer, Double>> myRewards;
	HashMap<Integer, HashMap<Integer, Double[]>> myTransitions;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;

		// Create and stock the states
		myStates = new ArrayList<State>();
		Task task;
		int i = 0;
		for (City city1 : topology) {
			for (City city2 : topology) {
				if (city2.id != city1.id) {
					task = new Task(i, city1, city2, td.reward(city1, city2), td.weight(city1, city2));
					myStates.add(new State(city1, task, i));
					i++;
				}
			}
			myStates.add(new State(city1, null, i));
			i++;
		}

		// Create the actions
		myActions = new ArrayList<ActionAtState>();
		int id = 0;

		for (City city : topology) {
			// Add an action for each city and decision
			myActions.add(new ActionAtState(true, city, id));
			id++;

			myActions.add(new ActionAtState(false, city, id));
			id++;

		}
		/*
		 * for (State myState : myStates) { // Add the action of accepting the task
		 * myActions.add(new ActionAtState(true, state.getCurrentTask().deliveryCity,
		 * id)); id++;
		 * 
		 * // Add the actions of refusing the task and moving to a neighboring city City
		 * myCity = myState.getCurrentCity(); for (City neighbor : myCity) {
		 * myActions.add(new ActionAtState(false, neighbor, id)); id++; } }
		 */

		myRewards = new HashMap<Integer, HashMap<Integer, Double>>();

		// Get the cost per km
		double costPerKm = agent.vehicles().get(0).costPerKm();

		// Build the rewards and transitions
		buildRewardsAndTransitions(topology, td, agent, costPerKm);

		// TODO : Build the transitions, Best, and V
		buildBestValueTable(topology, td, agent, costPerKm, discount);

	}

	private void buildBestValueTable(Topology topology, TaskDistribution td, Agent agent, double costPerKm, Double discount) {
		valueTable = new Double[myStates.size()];
		Arrays.fill(valueTable, new Random().nextDouble());
		best_actions = new ActionAtState[myStates.size()];
		
		
		ArrayList<Double> qTable;
		Integer action_id;
		Integer state_id;
		double dotProduct;
		double maxi = -Double.MAX_VALUE;
		double qElement;
		double diff = 0;
		double denominator;
		double normalizedDiff = 0;
		double TRESHOLD_MIN = 0;
		
		
		ActionAtState bestAction = null;
		
		Double[] oldValueTable = new Double[myStates.size()];
		Arrays.fill(oldValueTable, new Random().nextDouble());

		int k=0;
		
		
		do{k++;
			diff = 0;
			oldValueTable = valueTable.clone();
			for (State state : myStates) {
				maxi = -Double.MAX_VALUE;
				qTable = new ArrayList<Double>(myActions.size());
				int ind = 0;
				state_id = state.getId();
				for (ActionAtState action : myActions) {
					action_id = action.getId();
					
					dotProduct = dotProduct(myTransitions.get(state_id).get(action_id), valueTable);
					if (myRewards.get(state_id).containsKey(action_id) ) {
						qElement = myRewards.get(state_id).get(action_id) + discount * dotProduct;
					}
					else {
						qElement = -Double.MAX_VALUE;
					}
					qTable.add(ind, qElement);
					if (qElement > maxi) {
						maxi = qElement;
						bestAction = action;
					}
					
					
					ind++;
				}
				
				valueTable[state_id] = maxi;
				best_actions[state_id] = bestAction;
				diff += (Math.pow(oldValueTable[state_id]- valueTable[state_id], 2));
				denominator = dotProduct(oldValueTable, oldValueTable);
				normalizedDiff = diff/denominator;
				
			}
		} while(normalizedDiff > TRESHOLD_MIN);
		System.out.println("Zero");
	}
	


	public double dotProduct(Double[] a , Double[] b) {
		double v = 0;
		for (int i=0; i < a.length; i++) {
			v += a[i] * b[i];
		}
		return v;
	}
	
	public void buildRewardsAndTransitions(Topology topology, TaskDistribution td, Agent agent, double costPerKm) {
		myTransitions = new HashMap<Integer, HashMap<Integer, Double[]>>();
		int state_id;
		int action_id;
		City source;
		City destination;

		for (State myState : myStates) {
			source = myState.getCurrentCity();
			if( myState.getCurrentTask() != null) {
				destination = myState.getCurrentTask().deliveryCity;
			}
			else {
				destination = null;
			}

			state_id = myState.getId();
			HashMap<Integer, Double> stateRewards = new HashMap<Integer, Double>();

			HashMap<Integer, Double[]> actionsProbabilities = new HashMap<Integer, Double[]>();

			for (ActionAtState action : myActions) {

				action_id = action.getId();
				
				if (source.hasNeighbor(action.getNextCity()) && !(action.getDecision())) {
					stateRewards.put(action_id, -costPerKm * source.distanceTo(action.getNextCity()));
				}

				if (destination != null && action.getDecision() && action.getNextCity().id == destination.id) {
					// If we accept the task
					stateRewards.put(action_id,
							td.reward(source, destination) - costPerKm * source.distanceTo(destination));
				}

				Double[] probabilities = new Double[myStates.size()];
				Arrays.fill(probabilities, 0.0);

				if (stateRewards.containsKey(action_id)) {

					for (State finalState : myStates) {
						if (action.getNextCity().id == finalState.getCurrentCity().id) {
							if (finalState.getCurrentTask() == null) {
							probabilities[finalState.getId()] = td.probability(finalState.getCurrentCity(),
									null);
							}
							else {
								probabilities[finalState.getId()] = td.probability(finalState.getCurrentCity(),
										finalState.getCurrentTask().deliveryCity);}

						}

					}
				}
				actionsProbabilities.put(action_id, probabilities);
			}
			myTransitions.put(state_id, actionsProbabilities);
			myRewards.put(state_id, stateRewards);

		}

	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		ActionAtState bestAction = null;
		if(availableTask == null) {
			for(State state:myStates) {
				if (state.getCurrentCity().id == vehicle.getCurrentCity().id && state.getCurrentTask()==null) {
					bestAction = best_actions[state.getId()];
					break;
				}
			}
			action = new Move(bestAction.getNextCity());
			
		}
		else {
			for(State state:myStates) {
				if (state.getCurrentTask() != null) {
					if (state.getCurrentCity().id ==availableTask.pickupCity.id & state.getCurrentTask().deliveryCity.id == availableTask.deliveryCity.id) {
						bestAction = best_actions[state.getId()];
						break;
					}
				}
			}
			if(bestAction.getDecision()) {
				action = new Pickup(availableTask);
				
			}
			else {
				action = new Move(bestAction.getNextCity());
			}
		}
		


		if (numActions >= 1) {
			System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit()
					+ " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
		}
		numActions++;

		return action;
	}



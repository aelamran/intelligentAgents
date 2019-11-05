package centralized;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Iterator;
import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Action;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedMain implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_setup;
	private long timeout_plan;
	private Double TRESHOLD_OLD_SOL = 0.4;
	private Double TRESHOLD_OTHER_NEIGHBOR = 0.1;
	private final int MAX_ITERATIONS = 50000;


	private Double COST_DIFFERENCE = 0.0;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
	}

	private Solution getClosestInitialSolution(List<Vehicle> myVehicles, TaskSet tasks) {
		Integer numberVehicles = myVehicles.size();
		Integer numberTasks = tasks.size();
		ArrayList<Map.Entry<Integer, Task>> nextActions = initializeNextActions(2 * numberTasks + numberVehicles);
		HashMap<Integer, City> vehicleCity = new HashMap<Integer, City>();
		HashMap<Integer, Integer> LastActionIdMap = new HashMap<Integer, Integer>();
		ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>(Arrays.asList(new Vehicle[2 * numberTasks]));
		ArrayList<City> cities = new ArrayList<City>(Arrays.asList(new City[2 * numberTasks]));
		HashMap<Integer, Integer> timesVehicles = new HashMap<Integer, Integer>();
		ArrayList<Integer> times = new ArrayList<Integer>(Arrays.asList(new Integer[2 * numberTasks]));

		for (int i = 0; i < numberVehicles; i++) {
			timesVehicles.put(i, 0);
		}

		for (int i = 0; i < numberVehicles; i++) {
			LastActionIdMap.put(i, 2 * numberTasks + i);
		}

		for (int i = 0; i < numberVehicles; i++) {
			Vehicle v = myVehicles.get(i);
			vehicleCity.put(i, v.homeCity());
		}

		Double minDistance = Double.MAX_VALUE;
		Double tmpDistance;
		int closestVehicleId = 0;
		Integer taskId;
		int timeAction;
		for (Task task : tasks) {
			taskId = task.id;

			minDistance = Double.MAX_VALUE;

			for (int i = 0; i < numberVehicles; i++) {
				tmpDistance = myVehicles.get(i).homeCity().distanceTo(task.pickupCity);
				if (tmpDistance < minDistance) {

					minDistance = tmpDistance;
					closestVehicleId = i;

				}
			}
			nextActions.set(LastActionIdMap.get(closestVehicleId), new SimpleEntry<Integer, Task>(taskId, task));
			nextActions.set(taskId, new SimpleEntry<Integer, Task>(numberTasks + taskId, task));
			nextActions.set(numberTasks + taskId, new SimpleEntry<Integer, Task>(null, task));
			LastActionIdMap.put(closestVehicleId, numberTasks + taskId);
			vehicles.set((taskId), myVehicles.get(closestVehicleId));
			cities.set(taskId, task.pickupCity);
			cities.set(numberTasks + taskId, task.deliveryCity);
			vehicleCity.put(closestVehicleId, task.deliveryCity);

			timeAction = timesVehicles.get(closestVehicleId) + 1;
			times.set(taskId, timeAction);
			times.set(numberTasks + taskId, timeAction + 1);
			timesVehicles.put(closestVehicleId, timeAction + 1);

		}
		return new Solution(nextActions, times, vehicles, numberTasks, numberVehicles, cities);
	}

	private Solution getInitialSolution(List<Vehicle> myVehicles, TaskSet tasks) {
		Integer numberTasks = tasks.size();
		Integer numberVehicles = myVehicles.size();

		HashMap<Integer, ArrayList<Task>> currentTasksOfVehicles = new HashMap<Integer, ArrayList<Task>>();

		ArrayList<Integer> times = new ArrayList<Integer>(Arrays.asList(new Integer[2 * numberTasks]));

		HashMap<Integer, Integer> LastActionIdMap = new HashMap<Integer, Integer>();
		ArrayList<Map.Entry<Integer, Task>> nextActions = initializeNextActions(2 * numberTasks + numberVehicles); // =
																													// new
																													// ArrayList<Map.Entry<Integer,
																													// Task>>(2
																													// *
																													// numberTasks
																													// +
																													// numberVehicles);

		ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>(Arrays.asList(new Vehicle[2 * numberTasks]));

		ArrayList<City> cities = new ArrayList<City>(Arrays.asList(new City[2 * numberTasks]));
		int timeAction = 1;
		Integer lastActionId = null;
		/*
		 * Go through all tasks and add them to the vehicle that has the space for them
		 */
		Iterator<Task> iterator = tasks.iterator();
		Task task = iterator.next();
		int v = 0;
		int t = 0;
		do {

			Vehicle vehicle = myVehicles.get(v);
			Integer carried = 0;
			if (vehicle.capacity() - carried >= task.weight) {
				if (!currentTasksOfVehicles.containsKey(vehicle.id())) {

					// Set the vehicle, pickup, and delivery

					nextActions.set(2 * numberTasks + v, new SimpleEntry<Integer, Task>(t, task));
					nextActions.set(t, new SimpleEntry<Integer, Task>(numberTasks + t, task));
					nextActions.set(numberTasks + t, new SimpleEntry<Integer, Task>(null, task));

					lastActionId = numberTasks + t;
					LastActionIdMap.put(v, lastActionId);
					cities.set(t, task.pickupCity);
					cities.set(numberTasks + t, task.deliveryCity);

					timeAction = 1;
					times.set(t, timeAction);
					times.set(numberTasks + t, timeAction + 1);
					timeAction += 2;

				} else {
					int lastActionOfVehicle = LastActionIdMap.get(v);

					// Set the last action, pickup, and delivery

					nextActions.set(lastActionOfVehicle, new SimpleEntry<Integer, Task>(t, task));
					nextActions.set(t, new SimpleEntry<Integer, Task>(numberTasks + t, task));
					nextActions.set(numberTasks + t, new SimpleEntry<Integer, Task>(null, task));

					lastActionId = numberTasks + t;

					timeAction = times.get(lastActionOfVehicle) + 1;
					times.set(t, timeAction);
					times.set(numberTasks + t, timeAction + 1);
					timeAction += 2;

					LastActionIdMap.put(v, lastActionId);

					cities.set(t, task.pickupCity);
					cities.set(numberTasks + t, task.deliveryCity);

				}

				vehicles.set(t, vehicle);

				if (currentTasksOfVehicles.containsKey(vehicle.id())) {
					ArrayList<Task> currentTasks = currentTasksOfVehicles.get(vehicle.id());
					currentTasks.add(task);
					currentTasksOfVehicles.put(vehicle.id(), currentTasks);
				} else {
					ArrayList<Task> currentTasks = new ArrayList<Task>(vehicle.getCurrentTasks());
					currentTasks.add(task);
					currentTasksOfVehicles.put(vehicle.id(), currentTasks);
				}
				if (iterator.hasNext()) {
					task = iterator.next();
					t++;
				} else {
					break;
				}
			}

			v++;
			if (v >= numberVehicles) {
				v = 0;
			}

		} while (v < numberVehicles);
		return new Solution(nextActions, times, vehicles, numberTasks, numberVehicles, cities);
	}

	private ArrayList<Entry<Integer, Task>> initializeNextActions(int length) {
		ArrayList<Entry<Integer, Task>> nextActions = new ArrayList<Map.Entry<Integer, Task>>(length);
		for (int i = 0; i < length; i++) {
			nextActions.add(new SimpleEntry<Integer, Task>(null, null));
		}
		return nextActions;
	}

	public Task getTaskById(TaskSet tasks, int id) {
		if (id >= tasks.size()) {
			id = id - tasks.size();
		}
		for (Task task : tasks) {
			if (task.id == id) {
				return task;
			}
		}
		return null;
	}

	public double getCost(TaskSet tasks, List<Vehicle> myVehicles, Solution solution) {
		return transformSolutionToPlans(tasks, myVehicles, solution).getValue();
	}

	public Map.Entry<List<Plan>, Double> transformSolutionToPlans(TaskSet tasks, List<Vehicle> myVehicles,
			Solution sol) {

		List<Plan> plans = new ArrayList<Plan>();
		int j = 0;
		int actionId = 0;
		ArrayList<City> cities = sol.getCities();
		double cost = 0.0;

		double distanceCrossed = 0.0;
		double distanceCrossedByVehicle = 0.0;

		for (int i = 0; i < sol.numberVehicles; i++) {
			City oldCity = myVehicles.get(i).homeCity();
			Plan plan = new Plan(oldCity);
			ArrayList<Map.Entry<Integer, Task>> nextActions = sol.getNextActions();

			// Get the action id
			actionId = 2 * sol.numberTasks + i;
			Vehicle v = myVehicles.get(i);

			ArrayList<Entry<Integer, Task>> actionsOfVehicle = getActionsOfVehicle(sol, i);

			for (int a = 0; a < actionsOfVehicle.size(); a++) {
				actionId = actionsOfVehicle.get(a).getKey();
				City newCity = cities.get(actionId);
				City lastCityOnPath = oldCity;

				for (City city : oldCity.pathTo(newCity)) {
					plan.appendMove(city);
					distanceCrossedByVehicle += lastCityOnPath.distanceTo(city);
					lastCityOnPath = city;
				}

				if (actionId < sol.getNumberTasks()) {
					plan.appendPickup(getTaskById(tasks, actionId));

				} else if (actionId < 2 * sol.getNumberTasks()) {
					plan.appendDelivery(getTaskById(tasks, actionId - sol.getNumberTasks()));

				} else {
					System.out.println("Error on index ");
				}
				oldCity = newCity;
			}

			plans.add(plan);
			cost += plan.totalDistance() * myVehicles.get(i).costPerKm();

			distanceCrossedByVehicle *= myVehicles.get(i).costPerKm();
			distanceCrossed += distanceCrossedByVehicle;
			distanceCrossedByVehicle = 0.0;

		}

		return new SimpleEntry(plans, cost);
	}

	public static String natureOfTask(Integer t, int numberTasks) {
		if (0 <= t && t < numberTasks) {
			return "PICKUP";
		} else {
			return "DELIVERY";
		}
	}

	public ArrayList<Map.Entry<Integer, Task>> getActionsOfVehicle(Solution sol, int v) {

		ArrayList<Map.Entry<Integer, Task>> actionsOfVehicle = new ArrayList<Map.Entry<Integer, Task>>();
		ArrayList<Map.Entry<Integer, Task>> nextActions = sol.getNextActions();
		int numberTasks = sol.getNumberTasks();

		Map.Entry<Integer, Task> currentActionMap = nextActions.get(2 * numberTasks + v);
		Integer idCurrentAction = currentActionMap.getKey();
		if (idCurrentAction == null) {
			return actionsOfVehicle;
		}
		actionsOfVehicle.add(currentActionMap);
		while (nextActions.get(idCurrentAction).getKey() != null) {
			currentActionMap = nextActions.get(idCurrentAction);
			idCurrentAction = currentActionMap.getKey();
			actionsOfVehicle.add(currentActionMap);
		}
		return actionsOfVehicle;
	}

	public HashSet<Solution> chooseNeighbors(List<Vehicle> myVehicles, TaskSet tasks, Solution oldSolution) {
		HashSet<Solution> neighbors = new HashSet<Solution>();
		Integer numberOftasks = oldSolution.getNumberTasks();
		Random r = new Random();
		// Choose a vehicle at random
		int v = 0;
		ArrayList<Map.Entry<Integer, Task>> nextActions = (ArrayList<Entry<Integer, Task>>) oldSolution.getNextActions()
				.clone();

		v = r.ints(0, (oldSolution.numberVehicles)).findFirst().getAsInt();
		Vehicle vehicle = myVehicles.get(v);

		// Change vehicle

		for (int vi = 0; vi < oldSolution.numberVehicles; vi++) {
			if (vi != v) {
				ArrayList<Map.Entry<Integer, Task>> myActions = getActionsOfVehicle(oldSolution, vi);
				if (myActions == null || myActions.isEmpty()) {
					continue;
				}
				int pickupPosition = 0;// = nextActions.get(2 * numberOftasks + vi).getKey();
				Map.Entry<Integer, Task> mapMyActions = myActions.get(pickupPosition);
				if (myActions.get(pickupPosition).getValue().weight <= vehicle.capacity()) {
					Solution newSol = changeVehicle(oldSolution, myVehicles, v, vi, mapMyActions, myActions,
							pickupPosition);
					if (newSol != null) {
						neighbors.add(newSol);
					}
				}
			}
		}

		// Change order of tasks
		int t = v;

		ArrayList<Entry<Integer, Task>> actionsOfVehicle = getActionsOfVehicle(oldSolution, v);
		int length = actionsOfVehicle.size();

		int action1;
		int action2;
		if (length >= 2) {
			for (int ti = 0; ti < length; ti++) {
				for (int tj = ti + 1; tj < length; tj++) {
					// Check that the two actions are not pickup and delivery of the same task
					action1 = actionsOfVehicle.get(ti).getKey();
					action2 = actionsOfVehicle.get(tj).getKey();
					if (Math.abs(action1 - action2) != oldSolution.getNumberTasks()) {

						Solution newSol = changeTaskOrder(oldSolution, tasks, myVehicles, v, action1, action2);

						if (newSol != null) {
							neighbors.add(newSol);
						}
					}

				}
			}
		}
		if (neighbors.isEmpty()) {
			System.out.println("empty");
		}

		return neighbors;

	}

	public boolean checkLoadConstraints(Solution sol, int v, int capacity) {

		ArrayList<Map.Entry<Integer, Task>> nextActions = sol.getNextActions();
		int numberTasks = sol.getNumberTasks();

		Integer currentActionId = nextActions.get(2 * numberTasks + v).getKey();
		Entry<Integer, Task> currentAction = nextActions.get(2 * numberTasks + v);

		int used = 0;
		while (nextActions.get(currentActionId).getKey() != null) {
			if (natureOfTask(currentActionId, numberTasks) == "PICKUP") {
				used += currentAction.getValue().weight;
				if (used > capacity) {
					return false;
				}
			} else {
				used -= currentAction.getValue().weight;
			}
			currentAction = nextActions.get(currentActionId);
			currentActionId = currentAction.getKey();
		}
		return true;
	}

	private Solution changeTaskOrder(Solution oldSolution, TaskSet tasks, List<Vehicle> myVehicles, int v, int ti,
			int tj) {

		int numberTasks = oldSolution.getNumberTasks();

		// Get task i and j
		Task taskI = ((ti < numberTasks) ? getTaskById(tasks, ti) : getTaskById(tasks, (ti - numberTasks)));
		Task taskJ = ((tj < numberTasks) ? getTaskById(tasks, tj) : getTaskById(tasks, (tj - numberTasks)));

		Solution currentSolution = (Solution) (oldSolution.clone());

		int tPrev = v;
		ArrayList<Map.Entry<Integer, Task>> nextActions = (ArrayList<Entry<Integer, Task>>) oldSolution.getNextActions()
				.clone();

		int t1 = nextActions.get(tPrev).getKey();
		int count = 1;

		int numberVehicles = oldSolution.getNumberVehicles();
		ArrayList<City> cities = (ArrayList<City>) oldSolution.getCities().clone();

		ArrayList<Integer> oldTimes = (ArrayList<Integer>) oldSolution.getTimes().clone();
		ArrayList<Vehicle> oldVehicles = oldSolution.getVehicles();
		// Check that the two actions don't represent the same task
		if (Math.abs(ti - tj) == numberTasks) {
			return currentSolution;
		}

		Entry<Integer, Task> afterTi = nextActions.get(ti);
		Entry<Integer, Task> afterTj = nextActions.get(tj);

		int beforeTi = getEntryBefore(nextActions, ti);
		int beforeTj = getEntryBefore(nextActions, tj);

		// If ti and tj are one after the other
		if (tj == afterTi.getKey()) {

			nextActions.set(ti, afterTj);
			nextActions.set(tj, new SimpleEntry<Integer, Task>(ti, taskI));
			if (beforeTi >= 0) {
				nextActions.set(beforeTi, new SimpleEntry<Integer, Task>(tj, taskJ));
			}

		} // If ti and tj are NOT one after the other
		else {
			nextActions.set(ti, afterTj);
			nextActions.set(tj, afterTi);
			if (beforeTi >= 0) {
				nextActions.set(beforeTi, new SimpleEntry<Integer, Task>(tj, taskJ));
			} else {
				System.out.println("before");
			}
			nextActions.set(beforeTj, new SimpleEntry<Integer, Task>(ti, taskI));

		}

		// check that we don't reverse pickup and delivery of same task

		int timeI = oldTimes.get(ti);
		oldTimes.set(ti, oldTimes.get(tj));
		oldTimes.set(tj, timeI);

		Solution newSolution = new Solution(nextActions, oldTimes, oldVehicles, numberTasks, numberVehicles, cities);

		// Check capacity of vehicle

		if (checkLoadConstraints(newSolution, v, myVehicles.get(v).capacity()) && newSolution.checkConstraints()) {

			return newSolution;
		} else {
			return null;
		}
	}

	private int getEntryBefore(ArrayList<Entry<Integer, Task>> nextActions, int ti) {
		Integer nextActionOne;
		for (int i = 0; i < nextActions.size(); i++) {
			nextActionOne = nextActions.get(i).getKey();
			if (nextActionOne != null && nextActionOne == ti) {
				return i;
			}
		}
		return -1;
	}

	private Solution changeVehicle(Solution oldSolution, List<Vehicle> myVehicles, int vehicleToAddTaskTo,
			int vehicleToRemoveFrom, Map.Entry<Integer, Task> actionToPick,
			ArrayList<Map.Entry<Integer, Task>> myActions, Integer pickupPosition) {

		Integer numberTasks = oldSolution.getNumberTasks();
		Integer numberVehicles = oldSolution.getNumberVehicles();
		ArrayList<Map.Entry<Integer, Task>> oldActions = oldSolution.getNextActions();
		ArrayList<Integer> times = oldSolution.getTimes();
		Vehicle myVehicle = myVehicles.get(vehicleToRemoveFrom);
		Vehicle nextVehicle = myVehicles.get(vehicleToAddTaskTo);

		// New solution parameters
		ArrayList<Integer> newTimes = (ArrayList<Integer>) times.clone();
		ArrayList<Map.Entry<Integer, Task>> newActions = (ArrayList<Entry<Integer, Task>>) oldActions.clone();
		ArrayList<Vehicle> newVehicles = (ArrayList<Vehicle>) oldSolution.getVehicles().clone();
		ArrayList<City> newCities = (ArrayList<City>) oldSolution.getCities().clone();

		Integer pickedActionId = actionToPick.getKey();
		Task pickedTask = actionToPick.getValue();
		Integer correspondingDeliveryPosition = new Integer(0);
		correspondingDeliveryPosition = myActions
				.indexOf(new SimpleEntry<Integer, Task>(pickedActionId + numberTasks, pickedTask));

		Integer correspondingDeliveryActionId = myActions.get(correspondingDeliveryPosition).getKey();

		Map.Entry<Integer, Task> currentAction = myActions.get(pickupPosition);
		Integer currentActionId = currentAction.getKey();

		Integer actionOccuringBefore = 2 * numberTasks + vehicleToRemoveFrom;
		Integer actionOccuringLater = myActions.get(pickupPosition + 1).getKey();
		Task currentTask = currentAction.getValue();
		if (pickupPosition + 1 == correspondingDeliveryPosition) {
			newActions.set(actionOccuringBefore, newActions.get(correspondingDeliveryActionId));
			
		} else {
			// before pickup
			newActions.set(actionOccuringBefore, newActions.get(pickedActionId));

			// dealing with delivery pointers for the vehicle we remove tasks from
			

			Integer actionOccuringBeforeDeliv = myActions.get(correspondingDeliveryPosition - 1).getKey();// oldActions.indexOf(currentActionDeliv);

			
			newActions.set(actionOccuringBeforeDeliv, newActions.get(correspondingDeliveryActionId));

		}


		// Times of vehicle to remove task from
		Integer actionToChangeTime = newActions.get(2 * numberTasks + vehicleToRemoveFrom).getKey();
		Integer timeIncrement = new Integer(1);
		while (actionToChangeTime != null) {
			newTimes.set(actionToChangeTime, timeIncrement);
			timeIncrement += 1;
			actionToChangeTime = newActions.get(actionToChangeTime).getKey();
		}


		// Adding these two Actions to the second vehicle
		// We have to add the pickup and related delivery taken from other vehicle to
		// the new one
		// We do that by adding it at the beginning of the task

		newActions.set(2 * numberTasks + vehicleToAddTaskTo,
				new SimpleEntry<Integer, Task>(pickedActionId, pickedTask));
		newActions.set(pickedActionId, new SimpleEntry<Integer, Task>(correspondingDeliveryActionId, pickedTask));

		newVehicles.set(pickedActionId, nextVehicle);

		newActions.set(correspondingDeliveryActionId, oldActions.get(2 * numberTasks + vehicleToAddTaskTo));
		newVehicles.set(correspondingDeliveryActionId, nextVehicle);

		// Times of vehicle to add task to
		Integer windowAction = oldActions.get(2 * numberTasks + vehicleToAddTaskTo).getKey();
		newTimes.set(pickedActionId, new Integer(1));
		newTimes.set(correspondingDeliveryActionId, new Integer(2));

		while (windowAction != null) {
			newTimes.set(windowAction, times.get(windowAction) + 2);
			windowAction = oldActions.get(windowAction).getKey();
		}

		Solution newSol = new Solution(newActions, newTimes, newVehicles, numberTasks, numberVehicles, newCities);
		if (newSol.checkConstraints()) {
			return newSol;
		} else {
			if (newSol.checkConstraints()) {
				return newSol;
			}
			return null;
		}

	}

	public List<Plan> plan(List<Vehicle> myVehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();
		Solution initialSolution = getClosestInitialSolution(myVehicles, tasks);
		Solution currentSolution = (Solution) (initialSolution.clone());
		// Implementing the algorithm
		int i = 0;
		
		Solution bestSolution = currentSolution.clone();
		Double minCost = getCost(tasks, myVehicles, bestSolution);
		double currentCost = getCost(tasks, myVehicles, currentSolution);
		while (i < MAX_ITERATIONS) {
			Solution oldSolution = currentSolution.clone();
			HashSet<Solution> neighbors = chooseNeighbors(myVehicles, tasks, currentSolution);
			currentSolution = localChoice(tasks, myVehicles, currentSolution, neighbors).clone();
			currentCost = getCost(tasks, myVehicles, currentSolution);
			if (currentCost < minCost ){
				minCost = currentCost;
				bestSolution = currentSolution;
			}

			i++;
		}

		if (currentCost> minCost){
			currentSolution = bestSolution;
		}
		System.out.println("min cost "+minCost);
		System.out.println("min cost "+getCost(tasks, myVehicles, currentSolution));
		
		List<Plan> plans = transformSolutionToPlans(tasks, myVehicles, currentSolution).getKey();

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");
		for (int vi = 0; vi < myVehicles.size(); vi++) {
			System.out.println(getActionsOfVehicle(currentSolution, vi));
		}
		return plans;
	}

	private Solution localChoice(TaskSet tasks, List<Vehicle> myVehicles, Solution oldSolution,
			HashSet<Solution> neighbors) {

		double oldCost = getCost(tasks, myVehicles, oldSolution);
		System.out.println("old cost " + oldCost);
		Solution newSol = oldSolution.clone();
		double newCost;
		for (Solution sol : neighbors) {
			newCost = getCost(tasks, myVehicles, sol);

			if (newCost < oldCost - COST_DIFFERENCE) {
				newSol = sol;
				oldCost = newCost;

			}
		}

		Random r = new Random();
		double proba = r.nextDouble();
		if (proba < TRESHOLD_OLD_SOL) {
			return oldSolution;
		} else if (proba < TRESHOLD_OLD_SOL + TRESHOLD_OTHER_NEIGHBOR) {
			// Choose random
			if (neighbors.isEmpty()) {
				System.out.println("empty");
				return oldSolution;
			}
			int index = r.nextInt(neighbors.size());
			Iterator<Solution> iter = neighbors.iterator();
			for (int i = 0; i < index; i++) {
				iter.next();
			}
			return iter.next().clone();

		} else {
			return newSol;
		}
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity)) {
				plan.appendMove(city);
			}

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path()) {
				plan.appendMove(city);
			}

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
}

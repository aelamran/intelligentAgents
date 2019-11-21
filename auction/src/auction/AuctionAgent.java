package auction;

//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
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
public class AuctionAgent implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private List<Vehicle> vehicles;

	private City currentCity;
	private Set<Task> tasksWon;
	private Set<Task> tasksWonByOther;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.tasksWon = new HashSet();// vehicle.getCurrentTasks();
		this.tasksWonByOther = new HashSet();
		this.vehicles = agent.vehicles();
		this.currentCity = vehicle.homeCity();

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		System.out.println(new ArrayList(Arrays.asList(bids)));
		if (winner == agent.id()) {
			tasksWon.add(previous);
			currentCity = previous.deliveryCity;
		} else {
			tasksWonByOther.add(previous);
		}
	}

	public boolean noTasks() {
		if (!tasksWon.isEmpty()) {
			return false;
		}
		boolean noTasks = true;
		for (Vehicle v : agent.vehicles()) {
			if (!v.getCurrentTasks().isEmpty()) {
				noTasks = false;
			}
		}

		return noTasks;
	}

	public double getCostOfTask(Task task, List<Vehicle> vehicles) {
		double cost = Double.MAX_VALUE;
		City pickupCity = task.pickupCity;
		double closestCityDistance = Double.MAX_VALUE;
		Vehicle chosenVehicle = vehicles.get(0);
		for (Vehicle v : vehicles) {
			double d = v.getCurrentCity().distanceTo(pickupCity);
			if (d < closestCityDistance && v.capacity() >= task.weight) {
				closestCityDistance = d;
				chosenVehicle = v;
			}
		}
		cost = chosenVehicle.costPerKm() * (closestCityDistance + pickupCity.distanceTo(task.deliveryCity));
		return cost;
	}

	public double getAddedCost() {
		return 0.0;
	}

	/**
	 * Method to estimate marginal cost of opponent
	 * 
	 * @return
	 */
	public double getCostOfOpponent(Task task, List<Vehicle> vehicles) {
		if (tasksWonByOther.isEmpty()){
			return getCostOfTask(task, vehicles);
		}
		else{
			//Sls sls = new Sls(topology, distribution);
			return getCostWithAddedTask(tasksWonByOther, task, vehicles);
		}

	}

	public double getCostWithAddedTask(Set<Task> tasks, Task task, List<Vehicle> vehicles) {
		Sls sls = new Sls(topology, distribution, agent);
		Set<Task> eventuallyWonTasks = cloneTasks(tasks);
		eventuallyWonTasks.add(task);
		Solution myCurrentSolution = sls.getBestSolution(vehicles, tasksWon);
		Solution myEventualSolution = sls.getBestSolution(vehicles, eventuallyWonTasks);
		return sls.getCost(eventuallyWonTasks, vehicles, myEventualSolution)
				- sls.getCost(tasksWon, vehicles, myCurrentSolution);
	}

	@Override
	public Long askPrice(Task task) {
		double marginalCost = 0.0;
		double bid;

		// Check that we have capacity
		boolean taskCanFit = false;
		for (Vehicle v : vehicles) {
			if (v.capacity() >= task.weight) {
				taskCanFit = true;
			}
		}
		// No vehicle can carry the task
		if (!taskCanFit) {
			return null;
		}
		// Now we have checked that the task can fit

		// If it's the first task
		if (noTasks()) {
			marginalCost = getCostOfTask(task, agent.vehicles());
			bid = marginalCost + getAddedCost();
			System.out.println("no tasks");
			System.out.println(marginalCost);
			return (long) Math.round(bid);
		}

		else {
			marginalCost = getCostWithAddedTask(tasksWon, task, agent.vehicles());
			bid = marginalCost + getAddedCost();
			System.out.println(bid);

			// We suppose that the agent has the same vehicles as ours
			double costOfOpponent = getCostOfOpponent(task, agent.vehicles());
			System.out.println(costOfOpponent);
			return (long) Math.round(bid);
			/*
			 * if (vehicle.capacity() < task.weight) return null;
			 * 
			 * long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity); long
			 * distanceSum = distanceTask + currentCity.distanceUnitsTo(task.pickupCity);
			 * marginalCost = Measures.unitsToKM(distanceSum * vehicle.costPerKm());
			 * 
			 * double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id); bid = ratio *
			 * marginalCost;
			 * 
			 * System.out.println(bid); return (long) Math.round(bid);
			 */
		}
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

		// System.out.println("Agent " + agent.id() + " has tasks " + tasks);

		Plan planVehicle1 = naivePlan(vehicle, tasks);

		List<Plan> plans = new ArrayList<Plan>();
		plans.add(planVehicle1);
		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

		return plans;
	}

	private Set<Task> cloneTasks(Set<Task> tasks) {
		HashSet<Task> newTasks = new HashSet<Task>();
		for (Task t : tasks) {
			Task newTask = new Task(t.id, t.pickupCity, t.deliveryCity, t.reward, t.weight);
			newTasks.add(newTask);
		}
		return newTasks;
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
}
package auction;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
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
	private double cumulatedCost;
	private double cumulatedCostOfOther;

	public static long timeout_plan;
	private long timeout_bid;
	private long timeout_setup;

	private City currentCity;
	private Set<Task> tasksWon;
	private Set<Task> tasksWonByOther;
	private ArrayList<Double> bidsByOther;
	private ArrayList<Double> marginByOther;
	private int firstSteps = 3;
	private int roundNumber = 0;
	private ArrayList<Double> costByOther;
	private long time_start_bid;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.tasksWon = new HashSet();// vehicle.getCurrentTasks();
		this.tasksWonByOther = new HashSet();
		this.vehicles = agent.vehicles();
		this.currentCity = vehicle.homeCity();
		this.bidsByOther = new ArrayList<Double>();
		this.marginByOther = new ArrayList<Double>();
		this.costByOther = new ArrayList<Double>();
		this.cumulatedCost = 0.0;
		this.cumulatedCostOfOther = 0.0;

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);

		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
	}

	public boolean otherBidNull(Long[] bids) {
		for (int i = 0; i < bids.length; i++) {
			if (i != agent.id()) {
				if (bids[i] == null) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {

		Double actualBidOther;

		boolean otherBidNull = otherBidNull(bids);
		if (winner == agent.id()) {
			if (!otherBidNull) {
				actualBidOther = (double) Collections.max(Arrays.asList(bids));
				bidsByOther.add(actualBidOther);
				marginByOther.add(actualBidOther - getCostOfOpponent(previous, vehicles));
				costByOther.add(getCostOfOpponent(previous, vehicles));
			}

			tasksWon.add(previous);

			Sls sls = new Sls(topology, distribution, tasksWon, timeout_bid, time_start_bid);
			Solution actualSolution = sls.getBestSolution(vehicles);
			setCumulatedCost(sls.getCost(vehicles, actualSolution));
			currentCity = previous.deliveryCity;
		} else {
			if (!otherBidNull) {
				actualBidOther = (double) Collections.min(Arrays.asList(bids));
				bidsByOther.add(actualBidOther);
				marginByOther.add(actualBidOther - getCostOfOpponent(previous, vehicles));
				costByOther.add(getCostOfOpponent(previous, vehicles));
			}

			tasksWonByOther.add(previous);

			Sls sls = new Sls(topology, distribution, tasksWonByOther, timeout_bid, time_start_bid);
			Solution actualSolution = sls.getBestSolution(vehicles);
			setCumulatedCostOfOther(sls.getCost(vehicles, actualSolution));

		}
		roundNumber++;
	}

	/**
	 * Checks if no tasks have been won
	 * 
	 * @return
	 */
	public boolean noTasks() {
		if (!tasksWon.isEmpty()) {
			return false;
		} else if (!tasksWonByOther.isEmpty()) {
			return false;
		}
		return true;
		/*
		 * boolean noTasks = true; for (Vehicle v : agent.vehicles()) { if
		 * (!v.getCurrentTasks().isEmpty()) { noTasks = false; } }
		 * 
		 * return noTasks;
		 */
	}

	/**
	 * get the cost of the first task of a vehicle
	 * 
	 * @param task
	 * @param vehicles
	 * @return
	 */
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

	/**
	 * Computes the bid added to the cost
	 * 
	 * @return
	 */
	public double computeMarginalBid() {
		if (tasksWon.isEmpty() && tasksWonByOther.isEmpty()) {
			return 0;
		}

		return 0.0;
	}

	/**
	 * Method to estimate marginal cost of opponent
	 * 
	 * @return
	 */
	public double getCostOfOpponent(Task task, List<Vehicle> vehicles) {
		//vehicles.get(0).homeCity() = 
		if (tasksWonByOther.isEmpty()) {
			return getCostOfTask(task, vehicles);
		} else {
			return getCostWithAddedTask(tasksWonByOther, task, vehicles, cumulatedCostOfOther);
		}

	}

	/**
	 * computes marginal cost of adding a task to an existing taskSet
	 * 
	 * @param tasks
	 * @param task
	 * @param vehicles
	 * @return
	 */
	public double getCostWithAddedTask(Set<Task> tasks, Task task, List<Vehicle> vehicles, double cost) {
		Set<Task> eventuallyWonTasks = cloneTasks(tasks);
		eventuallyWonTasks.add(task);
		Sls slsNew = new Sls(topology, distribution, eventuallyWonTasks, timeout_bid, time_start_bid);
		Solution myEventualSolution = slsNew.getBestSolution(vehicles);
		return slsNew.getCost(vehicles, myEventualSolution) - cost;
	}

	/**
	 * Computes the bid
	 * 
	 * @param task
	 * @return
	 */
	@Override
	public Long askPrice(Task task) {
		this.time_start_bid = System.currentTimeMillis();
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

		
		if (roundNumber < firstSteps || bidsByOther.isEmpty()) {
			marginalCost = getCostWithAddedTask(tasksWon, task, agent.vehicles(), cumulatedCost);
			bid = marginalCost + computeMarginalBid();
			if (bid <= 0){
				bid = getCostOfOpponent(task, agent.vehicles())* 0.8;
			}
			return (long) Math.round(bid);
		} else {
			marginalCost = getCostWithAddedTask(tasksWon, task, agent.vehicles(), cumulatedCost);
			bid = marginalCost + computeMarginalBid();

			// We suppose that the agent has the same vehicles as ours

			LinearRegression linReg = new LinearRegression(costByOther.toArray(new Double[costByOther.size()]),
					bidsByOther.toArray(new Double[bidsByOther.size()]));
			double costOfOpponent = getCostOfOpponent(task, agent.vehicles());
			if (costOfOpponent < 0) {
				costOfOpponent = 0.0;
			}
			double otherBid = linReg.predict(costOfOpponent);

		
			double lowestBidOfOther = costOfOpponent * (linReg.slope() - linReg.slopeStdErr()) + linReg.intercept();// -linReg.interceptStdErr();

			double highestBidOfOther = costOfOpponent * (linReg.slope() + linReg.slopeStdErr()) + linReg.intercept();// +
																														// linReg.interceptStdErr();

			if (marginalCost < lowestBidOfOther) {
				if (lowestBidOfOther / otherBid >= 0.9) {
					if (marginalCost > lowestBidOfOther * 0.9) {
						bid = (marginalCost + lowestBidOfOther) / 2.0;
					} else {
						bid = lowestBidOfOther * 0.9;
					}
				} else {
					bid = lowestBidOfOther;
				}
			} else {
				// TODO : future maybe?
				int maxId;
				if (tasksWon.isEmpty()) {
					maxId = 1;
				} else {
					maxId = Collections.max(tasksWon, new Comparator<Task>() {
						@Override
						public int compare(Task t1, Task t2) {

							return new Integer(t1.id).compareTo(t2.id);
						}
					}).id + 1;
				}
				Task probableTask = getMostProbableTask(maxId);

				// Adding the most probable task
				Set<Task> eventuallyWonTasks = cloneTasks(tasksWon);
				eventuallyWonTasks.add(probableTask);

				Sls slsNew = new Sls(topology, distribution, eventuallyWonTasks, timeout_bid, time_start_bid);
				Solution myEventualSolution = slsNew.getBestSolution(vehicles);

				// Estimate difference of cost with and without current task
				double addedCost = getCostWithAddedTask(eventuallyWonTasks, task, vehicles,
						slsNew.getCost(vehicles, myEventualSolution));

				if (addedCost <= 0.0) {
					bid = (marginalCost + addedCost) * 0.5;
				} else {
					bid = (marginalCost + lowestBidOfOther) / 2;
				}
			}


			return (long) Math.round(bid);

			
		}
	}

	private Task getMostProbableTask(int id) {
		double maxProba = 0.0;
		Task mostProbableTask = null;
		for (City from : topology.cities()) {
			for (City to : topology.cities()) {
				double proba = distribution.probability(from, to);
				if (maxProba < proba) {
					maxProba = proba;
					mostProbableTask = new Task(id, from, to, distribution.reward(from, to),
							distribution.weight(from, to));
				}
			}
		}

		return mostProbableTask;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {


		Sls sls = new Sls(topology, distribution, tasks, timeout_plan, System.currentTimeMillis());
		List<Plan> plans = sls.plan(vehicles);
		for (Plan p : plans) {
			System.out.println("plan x distance " + p.totalDistance() * 5);

		}
		System.out.println("My reward" + tasks.rewardSum());
		return plans;

		/*
		 * Plan planVehicle1 = naivePlan(vehicle, tasks);
		 * 
		 * List<Plan> plans = new ArrayList<Plan>(); plans.add(planVehicle1); while
		 * (plans.size() < vehicles.size()) plans.add(Plan.EMPTY);
		 * 
		 * return plans;
		 */
	}

	public static Set<Task> cloneTasks(Set<Task> tasks) {
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

	public double getCumulatedCost() {
		return cumulatedCost;
	}

	public void setCumulatedCost(double cumulatedCost) {
		this.cumulatedCost = cumulatedCost;
	}

	public double getCumulatedCostOfOther() {
		return cumulatedCostOfOther;
	}

	public void setCumulatedCostOfOther(double cumulatedCostOfOther) {
		this.cumulatedCostOfOther = cumulatedCostOfOther;
	}
}

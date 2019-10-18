package deliberativeAgent;
import java.util.*;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import logist.plan.Action;
import logist.plan.Action.Pickup;
import logist.plan.Action.Delivery;


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
	
	public boolean visitedState(List<State> visited, State currentState) {
		
		for(State state:visited) {
			
			
			if(state.getAvailableTasks().equals(currentState.getAvailableTasks()) &&
					state.getCurrentCity().equals(currentState.getCurrentCity()) &&
					state.getCurrentTasks().equals(currentState.getCurrentTasks())) {
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
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			int V = tasks.size();
			int i =0;
			State initialState = new State(vehicle.getCurrentTasks(), tasks, vehicle.getCurrentCity(), i);
			i++;
			
			List<State> visitedState = new ArrayList<State>();
			LinkedList<State> queue = new LinkedList<State>();
			LinkedList<City> cities = new LinkedList<City>();
			HashMap <Integer, State> parentStates = new HashMap<Integer, State>();
			HashMap <Integer, Action> parentAction = new HashMap<Integer, Action>();
			LinkedList<Action> bestPath = new LinkedList<Action>();
			double tmpCost = 0.0;
			double finalCost = Double.MAX_VALUE;
			visitedState.add(initialState);
			queue.add(initialState);
			
			while(queue.size() != 0)
			{
				
				initialState = queue.poll();				
				if(initialState.isGoalState()){
					
					LinkedList<Action> path = new LinkedList<Action>();
					State parentState = initialState;
					State interState = initialState;
					while(parentStates.containsKey(parentState.getId())){
						Action toAdd = parentAction.get(parentState.getId());
						path.add(0, toAdd);
						cities.add(0, parentState.getCurrentCity());
						System.out.println(parentAction.get(parentState.getId()));
						tmpCost += vehicle.costPerKm()*interState.getCurrentCity().distanceTo(parentState.getCurrentCity());
						parentState = parentStates.get(parentState.getId());
						interState = parentState;
					}
					if(tmpCost<finalCost) {
						finalCost = tmpCost;
						bestPath = path;
					}
					
				}
				
				
				List<State> neighboursState = new ArrayList<State>();
				for (Task task: initialState.getCurrentTasks()) {
					TaskSet carriedTasks = initialState.getCurrentTasks().clone();
					carriedTasks.remove(task);
					State tmpState = new State(carriedTasks, initialState.getAvailableTasks(), task.deliveryCity, i);
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
						neighboursState.add(tmpState);
						parentStates.put(i, initialState);
						parentAction.put(i, new Pickup(task));
						i++;
						
					}
				}
				

				
				for(State state: neighboursState) {
					if(!visitedState(visitedState, state)) {
						visitedState.add(state);
						queue.add(state);
						System.out.println("visited state");
						//System.out.println(state.getId());
					}
				}
				
			}
			
			
			
			
			// ...
			
			
			City oldCity = vehicle.getCurrentCity();
			plan = new Plan(oldCity);
			for (int j=0; j<bestPath.size(); j++) {
				Action action = bestPath.get(j);
				for (City city : oldCity.pathTo(cities.get(j))) {
					plan.appendMove(city);
				}
				oldCity = cities.get(j);
				plan.append(action);
				
			}

			
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		
		return plan;
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

package centralized;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

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
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
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

    private Solution getInitialSolution(List<Vehicle> myVehicles, TaskSet tasks) {
        Integer numberTasks = tasks.size();
        Integer numberVehicles = myVehicles.size();
        ArrayList<Integer> times = new ArrayList<Integer>(Arrays.asList(new Integer[2 * numberTasks ]));
        HashMap<Integer, TaskSet> currentTasksOfVehicles = new HashMap();
        HashMap<Integer, Integer> LastActionIdMap = new HashMap();
        ArrayList<Integer> nextActions = new ArrayList<Integer>(Arrays.asList(new Integer[2 * numberTasks + numberVehicles]));
        ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>(Arrays.asList(new Vehicle[2 * numberTasks]));
                                                                                                                        
        ArrayList<City> cities = new ArrayList<City>(Arrays.asList(new City[2 * numberTasks]));
        int timeAction = 1;
        Integer lastActionId = null;
        /*  Go through all tasks and add them to the vehicle that has the space for them*/
        Iterator<Task> iterator =  tasks.iterator();
        Task task = iterator.next();
        int v =0;
        int t = 0;
        	do {
        		
                Vehicle vehicle = myVehicles.get(v);
                Integer carried ;
                if (currentTasksOfVehicles.containsKey(vehicle.id()) ){
                    carried = currentTasksOfVehicles.get(vehicle.id()).weightSum();
                }
                else{
                    carried = 0;
                }
                if (vehicle.capacity() - carried >= task.weight){
                    if (!currentTasksOfVehicles.containsKey(vehicle.id())){
                        //nextActions.set(2*numberTasks + v, new Action.Pickup(task));
                        //nextActions.set(t, new Action.Delivery(task));
                        
                        nextActions.set(2*numberTasks +v, t);
                        nextActions.set(numberTasks+t, null);
                        lastActionId = numberTasks + t ;
                        LastActionIdMap.put(v, lastActionId);
                        cities.set(t, task.pickupCity);
                        cities.set(numberTasks+t, task.deliveryCity);
                    }
                    else{
                        
                        nextActions.set(LastActionIdMap.get(v), t);
                        nextActions.set(numberTasks+t, null);
                        lastActionId = numberTasks + t ;
                        LastActionIdMap.put(v, lastActionId);
                        //nextActions.set(lastActionId, new Action.Pickup(task));
                        //lastActionId = t;
                        //nextActions.set(lastActionId, new Action.Delivery(task));
                        //lastActionId = numberTasks + t;

                        //TODOOOO
                        cities.set(t, task.pickupCity);
                        cities.set(LastActionIdMap.get(v), task.deliveryCity);
                    }
                    
                    vehicles.set(t, vehicle);
                    
                    
                    
                    times.set(t, timeAction);
                    times.set(numberTasks+t, timeAction+1);
                    timeAction += 2;

                    //vehicle.getCurrentTasks().add(task);  
                     
                    if (currentTasksOfVehicles.containsKey(vehicle.id()) ){
                        TaskSet currentTasks = currentTasksOfVehicles.get(vehicle.id());
                        currentTasks.add(task);
                        currentTasksOfVehicles.put(vehicle.id(), currentTasks);
                    }
                    else
                    {
                        TaskSet currentTasks = vehicle.getCurrentTasks();
                        currentTasks.add(task);
                        currentTasksOfVehicles.put(vehicle.id(), currentTasks);
                    }
                    //break;
                    if(iterator.hasNext()) {
                    	task = iterator.next();
                    	t++;
                    }
                    else {
                    	break;
                    }     
                }
                else{
                    continue;
                }
                v++;
                if (v >= numberVehicles){
                    v = 0;
                }
            
            //t++;
        }while(v<numberVehicles);
        return new Solution(nextActions, times, vehicles, numberTasks, numberVehicles, cities);
    }

    //public or private for the following method ? // TODO
    //public List<Plan> transformSolutionToPlans(List<Vehicle> myVehicles, Solution sol){
        //ArrayList<Action> nextActionsFinal = sol.nextActions
        //List<Plan> plans = new ArrayList<Plan>();
        /*int j = 0;
        int taskId = 0;
        for(int i=0; i<sol.numberVehicles; i++){
            City oldCity = myVehicles.get(i).homeCity();
            Plan plan = new Plan(oldCity);
            ArrayList<Action> nextActions = sol.getNextActions();
            taskId = nextActions.get(2 * sol.numberTasks + i).task.id;
            while(nextActions.get(j) != null){
                City newCity = cities.get(j);
                plan.append(nextActions.get(j));
            }
        }*/
        
        //ArrayList<Integer> nextActions = sol.nextActions;
        //for(int i=0; i<sol.numberVehicles; i++){
            //City oldCity = sol.vehicles.get(sol.numberTasks).getCurrentCity();
            // Get the city of the vehicle first
          //  City oldCity = myVehicles.get(i).homeCity();
        
            // Create a plan for each vehicle
            //Plan plan = new Plan(oldCity);
                //Vehicle tmp_vehicle = sol.vehicles.get(sol.numberTasks);
            //if(iterator.hasNext())
            /*Action currentAction = sol.nextActions.get(2 * sol.numberTasks + i);
            for (City city : oldCity.pathTo(currentAction.)) {
				plan.appendMove(city);
			}
            plan.appendMove(city);
            int nextTime = sol.numberTasks + i;
            while(nextActions.get(nextTime) != null){
                nextTime = sol.times.get(nextTime);
                Action nextnextAction = sol.nextActions.get(nextTime);
            }

            
        }
        



*/



    //    return plans;
  //  }
    
    @Override
    /*public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        //Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

        Solution initialSolution = getInitialSolution(vehicles, tasks); 
        List<Plan> plans = new ArrayList<Plan>();
        plans = naivePlan(vehicles, tasks);
        /*plans.add(planVehicle1);
        while (plans.size() < vehicles.size()) {
            plans.add(Plan.EMPTY);
        }
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");
        
        return plans;
    }*/
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        //long time_start = System.currentTimeMillis();
        
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        Solution initialSolution = getInitialSolution(vehicles, tasks);

        //Solution currentSolution = (Solution) (initialSolution.clone());
        // Implementing the algorithm
        //int i = 0;
        //final int MAX_ITERATIONS = 10000;
        /*while(i < MAX_ITERATIONS){
            Solution oldSolution = currentSolution.clone();
            HashSet<Solution> neighbors = chooseNeighbors(oldSolution);
            currentSolution = localChoice(oldSolution, neighbors);
            i++;
        }*/


        //Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

        //List<Plan> plans = transformSolutionToPlans(myVehicles, currentSolution);
//            new ArrayList<Plan>();
        /*plans.add(planVehicle1);
        while (plans.size() < vehicles.size()) {
            plans.add(Plan.EMPTY);
        }
        */
        /*long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");
        
        return plans;
    */
            long time_start = System.currentTimeMillis();
        
    //		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
            Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);
    
            List<Plan> plans = new ArrayList<Plan>();
            plans.add(planVehicle1);
            while (plans.size() < vehicles.size()) {
                plans.add(Plan.EMPTY);
            }
            
            long time_end = System.currentTimeMillis();
            long duration = time_end - time_start;
            System.out.println("The plan was generated in " + duration + " milliseconds.");
            
            return plans;
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

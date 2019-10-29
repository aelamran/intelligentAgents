package centralized;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
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

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        //Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

        Solution initialSolution = getInitialSolution(vehicles, tasks); 
        List<Plan> plans = new ArrayList<Plan>();
        /*plans.add(planVehicle1);
        while (plans.size() < vehicles.size()) {
            plans.add(Plan.EMPTY);
        }*/
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");
        
        return plans;
    }

    private Solution getInitialSolution(List<Vehicle> myVehicles, TaskSet tasks) {
        Integer numberTasks = tasks.size();
        Integer numberVehicles = myVehicles.size();

        HashMap<Integer, TaskSet> currentTasksOfVehicles = new HashMap();

        ArrayList<Action> nextActions = new ArrayList<Action>(Arrays.asList(new Action[2 * numberTasks + numberVehicles]));// new
                                                                                                                        // ArrayList<Action>(2*numberTasks
                                                                                                                        // +
                                                                                                                        // numberVehicles);
        ArrayList<Integer> times = new ArrayList<Integer>(Arrays.asList(new Integer[2 * numberTasks ]));//new ArrayList<Integer>(2*numberTasks);
        ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>(Arrays.asList(new Vehicle[numberTasks]));
        int t = 1;
        int timeAction = 1;
        Integer lastActionId = null;
        /*  Go through all tasks and add them to the vehicle that has the space for them*/
        for (Task task : tasks){
            int v = 0;
            while (v< numberVehicles){
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
                        nextActions.set(2*numberTasks + v, new Action.Pickup(task));
                        nextActions.set(t-1, new Action.Delivery(task));
                        lastActionId = numberTasks+t-1;
                    }
                    else{
                        
                        nextActions.set(lastActionId, new Action.Pickup(task));
                        lastActionId = t-1;
                        nextActions.set(lastActionId, new Action.Delivery(task));
                        lastActionId = numberTasks + t-1;
                    }
                    
                    vehicles.set(t-1, vehicle);
                    
                    times.set(t-1, timeAction);
                    times.set(numberTasks+t-1, timeAction+1);
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
                    break;
                }
                v ++;
            }
            t++;
        }
        return new Solution(nextActions, times, vehicles, numberTasks, numberVehicles);
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

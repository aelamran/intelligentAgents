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

        HashMap<Integer, ArrayList<Task>> currentTasksOfVehicles = new HashMap<Integer,ArrayList<Task>>();

        ArrayList<Integer> times = new ArrayList<Integer>(Arrays.asList(new Integer[2 * numberTasks ]));

        HashMap<Integer, Integer> LastActionIdMap = new HashMap<Integer,Integer>();
        ArrayList<Map.Entry<Integer, Task>> nextActions = new ArrayList<Map.Entry<Integer, Task>>(2 * numberTasks + numberVehicles);
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
                Integer carried = 0 ;
                if (vehicle.capacity() - carried >= task.weight){
                    if (!currentTasksOfVehicles.containsKey(vehicle.id())){
                        
                        // Set the vehicle, pickup, and delivery


                        nextActions.set(2*numberTasks +v, new SimpleEntry<Integer, Task> (t, task));
                        nextActions.set(t,new SimpleEntry<Integer, Task> (numberTasks+t, task));
                        nextActions.set(numberTasks+t,new SimpleEntry<Integer, Task> (null, task));



                        lastActionId = numberTasks + t ;
                        LastActionIdMap.put(v, lastActionId);
                        cities.set(t, task.pickupCity);
                        cities.set(numberTasks+t, task.deliveryCity);
                        
                    }
                    else{
                        int lastActionOfVehicle = LastActionIdMap.get(v);
                        
                        // Set the last action, pickup, and delivery



                        nextActions.set(lastActionOfVehicle, new SimpleEntry<Integer, Task> (t, task));
                        nextActions.set(t,new SimpleEntry<Integer, Task> (numberTasks+t, task));
                        nextActions.set(numberTasks+t,new SimpleEntry<Integer, Task> (null, task));

                        lastActionId = numberTasks + t ;
                        LastActionIdMap.put(v, lastActionId);
                        
                        //TODOOOO
                        cities.set(t, task.pickupCity);
                        cities.set(numberTasks + t, task.deliveryCity);
                    }
                    
                    vehicles.set(t, vehicle);
                    
                    
                    
                    times.set(t, timeAction);
                    times.set(numberTasks+t, timeAction+1);
                    timeAction += 2;

                    //vehicle.getCurrentTasks().add(task);  
                     
                    if (currentTasksOfVehicles.containsKey(vehicle.id()) ){
                        ArrayList<Task> currentTasks = currentTasksOfVehicles.get(vehicle.id());
                        currentTasks.add(task);
                        currentTasksOfVehicles.put(vehicle.id(), currentTasks);
                    }
                    else
                    {
                        ArrayList<Task> currentTasks = new ArrayList<Task>(vehicle.getCurrentTasks());
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

    public Task getTaskById(TaskSet tasks, int id){
        for (Task task : tasks){
            if (task.id == id){
                return task;
            }
        }
        return null;
    }
    public List<Plan> transformSolutionToPlans(TaskSet tasks, List<Vehicle> myVehicles, Solution sol){
        List<Plan> plans = new ArrayList<Plan>();
        int j = 0;
        int actionId = 0;
        ArrayList<City> cities = sol.getCities();
        for(int i=0; i<sol.numberVehicles; i++){
            City oldCity = myVehicles.get(i).homeCity();
            Plan plan = new Plan(oldCity);
            ArrayList<Map.Entry<Integer, Task>> nextActions = sol.getNextActions();

            
            // Get the action id
            actionId = 2 * sol.numberTasks + i ;
            while(nextActions.get(actionId) != null){
                actionId = nextActions.get(actionId).getKey();
                //actionId = nextActions.get(actionId);
                City newCity = cities.get(actionId);
                //CityTaskId.get()
                //City newCity = CityTaskId.get(actionId).

                for (City city : oldCity.pathTo(newCity)) {
                    plan.appendMove(city);
                }

                if (actionId < sol.getNumberTasks()){
                    plan.appendPickup(nextActions.get(actionId).getValue());
                    //plan.appendPickup()
                }
                else if (actionId < 2 * sol.getNumberTasks()){
                    //plan.appendDelivery(getTaskById(tasks, actionId - sol.getNumberTasks()));
                    plan.appendDelivery(nextActions.get(actionId- sol.getNumberTasks()).getValue());
                }
                else{
                    System.out.println("Error on index ");
                }
                oldCity = newCity;

            }
            plans.add(plan);
        }
        return plans;              
    }

    public String natureOfTask(Integer t, int numberTasks){
        if (0<=t && t<numberTasks){
            return "PICKUP";
        }
        else{
            return "DELIVERY";
        }
    }

    public ArrayList<Map.Entry<Integer, Task>> getActionsOfVehicle(Solution sol, int v){

        ArrayList<Map.Entry<Integer, Task>> actionsOfVehicle = new ArrayList<Map.Entry<Integer, Task>>();
        ArrayList<Map.Entry<Integer, Task>> nextActions = sol.getNextActions();
        int numberTasks = sol.getNumberTasks();

        Map.Entry<Integer, Task> currentActionMap = nextActions.get(2*numberTasks + v);
        Integer idCurrentAction = currentActionMap.getKey();
        actionsOfVehicle.add(currentActionMap);
        while(!nextActions.get(idCurrentAction).getKey().equals(null)){
            currentActionMap = nextActions.get(idCurrentAction);
            idCurrentAction = currentActionMap.getKey();
            actionsOfVehicle.add(currentActionMap);
        }
        return actionsOfVehicle;
    }
        
    public HashSet<Solution> chooseNeighbors(List<Vehicle> myVehicles, TaskSet tasks, Solution oldSolution){
        HashSet<Solution> neighbors = new HashSet<Solution>();
        Integer numberOftasks = oldSolution.getNumberTasks();
        Random r = new Random();
        // Choose a vehicle at random
        int v = 0;
        ArrayList<Map.Entry<Integer, Task>> nextActions = oldSolution.getNextActions();

        do{
            v =  r.ints(0, (oldSolution.numberVehicles + 1)).findFirst().getAsInt();
        }while (nextActions.get(v) == null);
        Vehicle vehicle = myVehicles.get(v);

        // Change vehicle
        for (int vi = 0; vi < oldSolution.numberVehicles; vi++){
            if (vi != v){
                ArrayList<Map.Entry<Integer, Task>> myActions = getActionsOfVehicle(oldSolution, v);
                int pickupPosition = nextActions.get(2*numberOftasks +vi).getKey();
                Map.Entry<Integer, Task> mapMyActions = myActions.get(pickupPosition);
                if (myActions.get(pickupPosition).getValue().weight <= vehicle.capacity() ){
                    Solution newSol = changeVehicle(oldSolution, myVehicles, v, vi, mapMyActions, myActions, pickupPosition);
                    neighbors.add(newSol);
                }
            }
        }

        // Change order of tasks
        int t = v;
        int length = 0;
        while(nextActions.get(t).getKey() != null){
            t = nextActions.get(t).getKey();
            length ++;
        }
        if (length >= 2){
            for (int ti = 0; ti<length; ti++){
                for (int tj = ti+1; tj<length; tj++){
                    Solution newSol = changeTaskOrder(oldSolution, tasks, myVehicles, v, ti, tj);
                    neighbors.add(newSol);
                }
            }
        }


        return neighbors;

    }


    //TODO
    private Solution changeTaskOrder(Solution oldSolution, TaskSet tasks, List<Vehicle> myVehicles, int v, int ti,
        int tj) {
            
        Solution currentSolution = (Solution) (oldSolution.clone());
        int tPrev = v;
        ArrayList<Map.Entry<Integer, Task>> nextActions = oldSolution.getNextActions();

        int t1 = nextActions.get(tPrev).getKey();
        int count = 1;
        while(count < ti){
            tPrev = t1;
            t1 = nextActions.get(t1).getKey();
            count++;
        }


        return null;
    }

    //TODOO
    private Solution changeVehicle(Solution oldSolution, List<Vehicle> myVehicles, int v, int vi,
     Map.Entry<Integer, Task> actionToPick, ArrayList<Map.Entry<Integer, Task>> myActions, Integer pickupPosition){


        Integer numberTasks = oldSolution.getNumberTasks();
        Integer numberVehicles = oldSolution.getNumberVehicles();
        ArrayList<Map.Entry<Integer, Task>> oldActions = oldSolution.getNextActions();
        ArrayList<Integer> times = oldSolution.getTimes();
        Vehicle myVehicle = myVehicles.get(vi);
        Vehicle nextVehicle = myVehicles.get(v);

        // New solution parameters
        ArrayList<Integer> newTimes = times;
        HashMap<Integer, Integer> LastActionIdMap = new HashMap<Integer,Integer>();
        ArrayList<Map.Entry<Integer, Task>> newActions = oldActions;
        ArrayList<Vehicle> newVehicles = new ArrayList<Vehicle>(Arrays.asList(new Vehicle[2 * numberTasks]));                                                                                                         
        ArrayList<City> newCities = new ArrayList<City>(Arrays.asList(new City[2 * numberTasks]));


        Integer pickedActionId = actionToPick.getKey();
        Task pickedTask = actionToPick.getValue();
        Integer correspondingDeliveryPosition = new Integer(0);
        for(Map.Entry<Integer, Task> currentMap: myActions){

            if(pickedTask.id == currentMap.getValue().id){
                if(natureOfTask(currentMap.getKey(), numberTasks).equals("DELIVERY")){
                    Integer correspondingDelivery = currentMap.getKey();
                    break;
                }
            }
            correspondingDeliveryPosition+=1;
        }
        Task correspondingDeliveryTask = myActions.get(correspondingDeliveryPosition).getValue();

        for (int j= pickupPosition; j<correspondingDeliveryPosition; j++){
            Map.Entry<Integer, Task> currentAction = myActions.get(j);
            Integer currentActionId = currentAction.getKey();
            Integer actionOccuringBefore = oldActions.indexOf(currentAction);
            Integer actionOccuringLater = oldActions.get(currentActionId).getKey();
            Task currentTask = currentAction.getValue();
            newActions.set(actionOccuringBefore, new  SimpleEntry<Integer, Task> (actionOccuringLater, currentTask));
            //newActions.remove(currentAction);
            newTimes.set(currentActionId, times.get(currentActionId)-1);
            if(natureOfTask(actionOccuringBefore, numberTasks).equals("PICKUP")){
                newCities.set(actionOccuringBefore, currentTask.pickupCity);
            }
            else{
                newCities.set(actionOccuringBefore, currentTask.deliveryCity);
            }
        }
        
        Integer nextActionToModify = correspondingDeliveryPosition;
        Map.Entry<Integer, Task> nextActionMap = oldActions.get(nextActionToModify);
        while(!oldActions.get(correspondingDeliveryPosition).getKey().equals(null)){
            Integer actionOccuringBeforeDeliveryId = oldActions.indexOf(nextActionMap);
            Task actionOccuringBeforeDelivery = oldActions.get(actionOccuringBeforeDeliveryId).getValue();
            nextActionToModify = oldActions.get(nextActionToModify).getKey();
            newActions.set(actionOccuringBeforeDeliveryId, new SimpleEntry<Integer, Task> (nextActionToModify, actionOccuringBeforeDelivery));
            if(natureOfTask(actionOccuringBeforeDeliveryId, numberTasks).equals("DELIVERY")){
                newCities.set(actionOccuringBeforeDeliveryId, actionOccuringBeforeDelivery.deliveryCity); 
            }
            else{
                newCities.set(actionOccuringBeforeDeliveryId, actionOccuringBeforeDelivery.pickupCity);
            }
            newTimes.set(nextActionToModify, times.get(nextActionToModify)-1);
        }

        // Adding these two Actions to the second vehicle
        // We have to add the pickup and related delivery taken from other vehicle to the new one
        // We do that by adding it at the beginning of the task 

        Integer correspondingDeliveryActionId = myActions.get(correspondingDeliveryPosition).getKey();
        newActions.set(2*numberTasks+v, new SimpleEntry<Integer, Task>(pickedActionId, pickedTask));
        newActions.set(pickedActionId, new SimpleEntry<Integer, Task>(myActions.get(correspondingDeliveryPosition).getKey(), pickedTask));
        newCities.set(pickedActionId, pickedTask.pickupCity);
        newVehicles.set(pickedActionId, nextVehicle);
        newActions.set(correspondingDeliveryActionId, oldActions.get(2*numberTasks+v));
        newCities.set(correspondingDeliveryActionId, myActions.get(correspondingDeliveryPosition).getValue().deliveryCity);
        newVehicles.set(correspondingDeliveryActionId, nextVehicle);

        Integer windowAction = oldActions.get(2*numberTasks+v).getKey();
        Integer timeIncrement = new Integer(1);
        while(!windowAction.equals(null)){
            newTimes.set(oldActions.get(windowAction).getKey(), timeIncrement);
            windowAction = oldActions.get(windowAction).getKey();
            timeIncrement+=1;
        }
        

        
        return new Solution(newActions, newTimes, newVehicles, numberTasks, numberVehicles, newCities);
    }
    public List<Plan> plan(List<Vehicle> myVehicles, TaskSet tasks) {
        //long time_start = System.currentTimeMillis();
        
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        Solution initialSolution = getInitialSolution(myVehicles, tasks);

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
            //Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);
    
            List<Plan> plans = transformSolutionToPlans(tasks, myVehicles, initialSolution);
                //new ArrayList<Plan>();
            /*plans.add(planVehicle1);
            while (plans.size() < vehicles.size()) {
                plans.add(Plan.EMPTY);
            }*/
            
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

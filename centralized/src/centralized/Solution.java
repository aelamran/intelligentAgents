package centralized;
import logist.topology.Topology.City;
import java.util.ArrayList;
import java.util.List;

import logist.plan.Action;
import logist.simulation.Vehicle;

/**
 * Class to represent a possible solution
 */
public class Solution {
    ArrayList<Action> nextActions;
    ArrayList<City> cities;
    ArrayList<Integer> times;
    List<Vehicle> vehicles;
    Integer numberTasks;
    Integer numberVehicles;

    public Solution(ArrayList<Action> nextActions, ArrayList<Integer> times, ArrayList<City> cities, List<Vehicle> vehicles,
            Integer numberTasks, Integer numberVehicles) {
        this.nextActions = nextActions;
        this.times = times;
        this.vehicles = vehicles;
        this.numberTasks = numberTasks;
        this.numberVehicles = numberVehicles;
        this.cities = cities;
    }



    
}
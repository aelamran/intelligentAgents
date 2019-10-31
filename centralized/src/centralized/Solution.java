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

    public Solution(ArrayList<Action> nextActions, ArrayList<Integer> times, List<Vehicle> vehicles,
            Integer numberTasks, Integer numberVehicles, ArrayList<City> cities) {
        this.nextActions = nextActions;
        this.times = times;
        this.vehicles = vehicles;
        this.numberTasks = numberTasks;
        this.numberVehicles = numberVehicles;
        this.cities = cities;
    }

    public Solution clone(){
        Solution sol = new Solution(nextActions, times, vehicles, numberTasks, numberVehicles, cities);
        return sol;
    }

    public ArrayList<Action> getNextActions() {
        return nextActions;
    }

    public void setNextActions(ArrayList<Action> nextActions) {
        this.nextActions = nextActions;
    }

    public ArrayList<City> getCities() {
        return cities;
    }

    public void setCities(ArrayList<City> cities) {
        this.cities = cities;
    }

    public ArrayList<Integer> getTimes() {
        return times;
    }

    public void setTimes(ArrayList<Integer> times) {
        this.times = times;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public Integer getNumberTasks() {
        return numberTasks;
    }

    public void setNumberTasks(Integer numberTasks) {
        this.numberTasks = numberTasks;
    }

    public Integer getNumberVehicles() {
        return numberVehicles;
    }

    public void setNumberVehicles(Integer numberVehicles) {
        this.numberVehicles = numberVehicles;
    }


    

    
}
package centralized;
import logist.topology.Topology.City;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import logist.task.*;

import logist.simulation.Vehicle;

/**
 * Class to represent a possible solution
 */
public class Solution {
    ArrayList<Map.Entry<Integer, Task>> nextActions;
    ArrayList<City> cities;
    ArrayList<Integer> times;
    List<Vehicle> vehicles;
    Integer numberTasks;
    Integer numberVehicles;

    public Solution(ArrayList<Map.Entry<Integer, Task>> nextActions, ArrayList<Integer> times, List<Vehicle> vehicles,
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



    public ArrayList<Map.Entry<Integer, Task>> getNextActions() {
        return nextActions;
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
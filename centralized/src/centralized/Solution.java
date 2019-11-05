package centralized;

import logist.topology.Topology.City;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import logist.task.*;

import logist.simulation.Vehicle;

/**
 * Class to represent a possible solution
 */
public class Solution {
    ArrayList<Map.Entry<Integer, Task>> nextActions;
    ArrayList<City> cities;
    ArrayList<Integer> times;
    ArrayList<Vehicle> vehicles;
    Integer numberTasks;
    Integer numberVehicles;

    public Solution(ArrayList<Map.Entry<Integer, Task>> nextActions, ArrayList<Integer> times,
            ArrayList<Vehicle> vehicles, Integer numberTasks, Integer numberVehicles, ArrayList<City> cities) {
        this.nextActions = nextActions;
        this.times = times;
        this.vehicles = vehicles;
        this.numberTasks = numberTasks;
        this.numberVehicles = numberVehicles;
        this.cities = cities;
    }

    public Solution clone() {
        Solution sol = new Solution(nextActions, times, vehicles, numberTasks, numberVehicles, cities);
        return sol;
    }

    public boolean checkConstraints() {
        boolean isCorrect = true;
        for (int i = 0; i < nextActions.size(); i++) {
            Integer nextActionOfI = nextActions.get(i).getKey();
            // check that time of pickup is < than time of delivery
            if (i < numberTasks) { // i is a pickup
                if (times.get(i) >= times.get(i + numberTasks)) {
                    return false;
                }
                
            }

            if (nextActionOfI == null) {
                continue;
            }
            // Check that all entries are pickups or deliveries
            if (nextActionOfI != null && nextActionOfI > 2 * numberTasks) {
                return false;
            }

            // Check that times go one by one
            // System.out.println(nextActionOfI+" "+i);
            if (i < times.size() && times.get(nextActionOfI) != times.get(i) + 1) {
                return false;
            }

            // Check that nextAction of i is different than i
            if (nextActionOfI == i) {
                return false;
            }
            

        }

        for (int v = 0; v < numberVehicles; v++) {
            Integer firstVehicleTask = nextActions.get(2 * numberTasks + v).getKey();
            // Check that all vehicles start with a pickup
            if (firstVehicleTask != null && firstVehicleTask > numberTasks) {
                return false;
            }

            // Check that all vehicles start at 1
            if (firstVehicleTask != null && times.get(firstVehicleTask) != 1) {
                return false;
            }
        }
        return isCorrect;
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

    public ArrayList<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(ArrayList<Vehicle> vehicles) {
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
package auction;

import logist.task.Task;

import java.util.HashSet;
import java.util.Set;

public class MyTaskSet {// <E> extends HashSet<E> {

    public Set<Task> tasks;

    @Override
    public Object clone() {
        HashSet<Task> newTasks = new HashSet<Task>();
        for (Task t : tasks) {
            Task newTask = new Task(t.id, t.pickupCity, t.deliveryCity, t.reward, t.weight);
            newTasks.add(newTask);
        }
        return newTasks;
    }

    public MyTaskSet(Set<Task> tasks) {
        this.tasks = tasks;
    }

    public Set<Task> getTasks() {
        return tasks;
    }

    public void setTasks(Set<Task> tasks) {
        this.tasks = tasks;
    }

}
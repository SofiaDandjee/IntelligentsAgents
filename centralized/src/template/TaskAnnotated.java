package template;
import logist.task.Task;

import java.util.Objects;

public class TaskAnnotated {
    Task task;
    private Planner.Activity activity;

    public TaskAnnotated(Task t, Planner.Activity act) {
        this.task = t;
        this.activity = act;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Planner.Activity getActivity() {
        return activity;
    }

    public void setActivity(Planner.Activity activity) {
        this.activity = activity;
    }

    @Override
    public String toString() {
        return "TaskAnnotated{" +
                "task=" + task +
                ", activity=" + activity +
                '}';
    }

    public boolean isPickup() { return activity == Planner.Activity.Pick;}

    public boolean isDeliver() {return activity == Planner.Activity.Deliver;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskAnnotated that = (TaskAnnotated) o;
        return task.equals(that.task) &&
                activity == that.activity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(task, activity);
    }
}

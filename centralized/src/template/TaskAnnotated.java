package template;
import logist.task.Task;

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
}

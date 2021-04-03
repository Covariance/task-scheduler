import java.util.ArrayList;
import java.util.Collection;
import ru.covariance.jb.scheduler.Task;

public class BasicTask implements Task {

  private final Runnable executor;
  private Collection<Task> deps;

  public BasicTask(final Runnable executor) {
    this.executor = executor;
    deps = new ArrayList<>();
  }

  public BasicTask(final Runnable executor, final Collection<Task> deps) {
     this.executor = executor;
     this.deps = deps;
  }

  @Override
  public void execute() {
    executor.run();
  }

  @Override
  public Collection<Task> dependencies() {
    return deps;
  }

  public void setDeps(final Collection<Task> deps) {
    this.deps = deps;
  }
}

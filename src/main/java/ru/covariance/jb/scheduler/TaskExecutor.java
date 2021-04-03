package ru.covariance.jb.scheduler;

import java.util.Collection;

public final class TaskExecutor {
  public void execute(final Collection<? extends Task> tasks) {
    new ExecutionController(tasks).execute();
  }
}

package ru.covariance.jb.scheduler;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

final class RunnableTask extends RecursiveAction {

  private final Task task;
  private final ExecutionController controller;

  public RunnableTask(final Task task, final ExecutionController controller) {
    this.task = task;
    this.controller = controller;
  }

  @Override
  protected void compute() {
    try {
      task.execute();
    } catch (Exception e) {
      controller.fail(task, e);
    }
    ForkJoinTask.invokeAll(controller.success(task));
  }
}

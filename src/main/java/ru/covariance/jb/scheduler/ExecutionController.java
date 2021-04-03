package ru.covariance.jb.scheduler;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.locks.ReentrantLock;

final class ExecutionController {

  private final ReentrantLock submitter = new ReentrantLock();
  private final DependencyTree dependencyTree;
  private final ForkJoinPool forkJoinPool;
  private CountDownLatch latch;
  private RuntimeException exception = null;

  public ExecutionController(final Collection<? extends Task> tasks) {
    dependencyTree = DependencyTree.build(this, tasks);
    forkJoinPool = ForkJoinPool.commonPool();
  }

  public void execute() {
    if (dependencyTree == null) {
      throw new TaskExecutionException("dependency tree is invalid");
    }

    latch = new CountDownLatch(dependencyTree.size());

    for (RecursiveAction action : dependencyTree.initial()) {
      forkJoinPool.invoke(action);
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      System.err.println("Interrupted until all tasks were finished: " + e.getMessage());
    }

    if (exception != null) {
      throw new TaskExecutionException("one of tasks threw exception", exception);
    }
  }

  public Collection<RecursiveAction> success(final Task task) {
    submitter.lock();
    latch.countDown();
    Collection<RecursiveAction> forkedActions = dependencyTree.update(task);
    submitter.unlock();
    return forkedActions;
  }

  public void fail(final Task task, final RuntimeException exception) {
    submitter.lock();
    this.exception = exception;
    while (latch.getCount() != 0) {
      latch.countDown();
    }
    submitter.unlock();
  }
}

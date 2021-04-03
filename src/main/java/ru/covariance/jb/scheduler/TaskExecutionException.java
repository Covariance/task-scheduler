package ru.covariance.jb.scheduler;

public final class TaskExecutionException extends RuntimeException {
  public TaskExecutionException(final String message) {
    super("Task execution exception: " + message);
  }

  public TaskExecutionException(final String message, final RuntimeException cause) {
    super("Task execution exception: " + message, cause);
  }
}

package ru.covariance.jb.scheduler;

import java.util.Collection;

public interface Task {
  void execute();

  Collection<? extends Task> dependencies();
}

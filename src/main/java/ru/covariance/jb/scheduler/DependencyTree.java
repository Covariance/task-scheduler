package ru.covariance.jb.scheduler;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class DependencyTree {

  private final ExecutionController owner;
  private final Map<Task, List<Task>> dependant;
  private final Map<Task, Integer> depsLeft;

  private DependencyTree(
      final ExecutionController owner,
      Map<Task, List<Task>> dependant,
      Map<Task, Integer> depsLeft) {
    this.owner = owner;
    this.dependant = dependant;
    this.depsLeft = depsLeft;
  }

  private static void addTransitive(final Map<Task, List<Task>> transitiveTasks, final Task task) {
    if (task != null && !transitiveTasks.containsKey(task)) {
      Collection<? extends Task> deps = task.dependencies();
      if (deps == null) {
        transitiveTasks.put(task, List.of());
      } else {
        List<Task> filtered = deps.stream().filter(Objects::nonNull).collect(Collectors.toList());
        transitiveTasks.put(task, filtered);
        for (Task transitive : deps) {
          addTransitive(transitiveTasks, transitive);
        }
      }
    }
  }

  public static DependencyTree build(
      final ExecutionController owner,
      final Collection<? extends Task> tasks
  ) {

    if (tasks == null || owner == null) {
      return null;
    }

    Map<Task, List<Task>> transitiveTasks = new HashMap<>();

    for (Task task : tasks) {
      addTransitive(transitiveTasks, task);
    }
    // In case some null values have been passed from deps
    transitiveTasks.remove(null);

    Map<Task, List<Task>> dependant = transitiveTasks.entrySet().stream()
        .flatMap(t -> t.getValue().stream().map(dt -> Map.entry(dt, t.getKey())))
        .collect(Collectors.groupingBy(
            Entry::getKey,
            Collectors.mapping(Entry::getValue, Collectors.toList())
        ));

    // If it was never met in dependencies, we need to add it manually
    transitiveTasks.keySet().stream()
        .filter(t -> !dependant.containsKey(t))
        .forEach(t -> dependant.put(t, List.of()));

    if (new CycleChecker<>(transitiveTasks).checkCycles()) {
      return null;
    }

    Map<Task, Integer> depsLeft = transitiveTasks.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            t -> t.getValue().size()
        ));

    return new DependencyTree(owner, dependant, depsLeft);
  }

  private RunnableTask constructRunnableTask(final Task task) {
    return new RunnableTask(task, owner);
  }

  private Collection<RecursiveAction> satisfiedFromStream(final Stream<Task> stream) {
    return stream
        .filter(t -> depsLeft.get(t) == 0)
        .map(this::constructRunnableTask)
        .collect(Collectors.toList());
  }

  public Collection<RecursiveAction> initial() {
    return satisfiedFromStream(dependant.keySet().stream());

  }

  public Collection<RecursiveAction> update(final Task task) {
    return satisfiedFromStream(
        dependant.get(task).stream().peek(t -> depsLeft.merge(t, 1, (a, b) -> (a - b)))
    );
  }

  public int size() {
    return dependant.size();
  }
}

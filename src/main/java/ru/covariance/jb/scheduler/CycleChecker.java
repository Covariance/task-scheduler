package ru.covariance.jb.scheduler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

final class CycleChecker<T> {

  private final Map<T, List<T>> graph;

  public enum VertexState {
    UNTOUCHED, ENTERED, LEFT
  }

  private Map<T, VertexState> state;

  public CycleChecker(final Map<T, List<T>> graph) {
    this.graph = graph;
  }

  private void constructState() {
    this.state = graph.keySet().stream().collect(Collectors.toMap(
        Function.identity(),
        t -> VertexState.UNTOUCHED)
    );
  }

  public boolean checkCycles() {
    constructState();
    for (Map.Entry<? extends T, VertexState> item : state.entrySet()) {
      if (item.getValue() == VertexState.UNTOUCHED) {
        if (dfs(item.getKey())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean dfs(T from) {
    switch (Objects.requireNonNull(state.put(from, VertexState.ENTERED))) {
      case LEFT -> {
        return false;
      }
      case ENTERED -> {
        return true;
      }
    }

    for (T next : graph.get(from)) {
      if (dfs(next)) {
        return true;
      }
    }

    state.put(from, VertexState.LEFT);
    return false;
  }
}

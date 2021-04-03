import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.covariance.jb.scheduler.TaskExecutionException;
import ru.covariance.jb.scheduler.Task;
import ru.covariance.jb.scheduler.TaskExecutor;

public class TestSuite {

  private final TaskExecutor executor = new TaskExecutor();

  @Test
  @Timeout(value = 10)
  public void noDepsTest() {
    AtomicInteger counter = new AtomicInteger();

    Assertions.assertDoesNotThrow(() -> executor.execute(
        IntStream.range(0, 100)
            .mapToObj(
                i -> new BasicTask(counter::incrementAndGet, List.of())
            )
            .collect(Collectors.toList())
    ));

    Assertions.assertEquals(100, counter.get());
  }

  @Test
  @Timeout(value = 10)
  public void bambooTest() {
    List<Integer> order = new ArrayList<>();

    List<Task> tasks = new ArrayList<>();
    tasks.add(new BasicTask(
        () -> order.add(0),
        List.of()
    ));
    for (int i = 1; i != 100; ++i) {
      int finalI = i;
      tasks.add(new BasicTask(
          () -> order.add(finalI),
          List.of(tasks.get(tasks.size() - 1))
      ));
    }

    Assertions.assertDoesNotThrow(() -> executor.execute(tasks));
    Assertions.assertEquals(100, order.size());
    for (int i = 0; i != 100; ++i) {
      Assertions.assertEquals(i, order.get(i));
    }
  }

  @Test
  @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
  public void cycledTest() {
    List<BasicTask> cycle = new ArrayList<>();
    for (int i = 0; i != 100; ++i) {
      cycle.add(new BasicTask(() -> System.out.println("Why am I launching?")));
    }
    for (int i = 0; i != 100; ++i) {
      cycle.get(i).setDeps(List.of(cycle.get((i + 1) % 100)));
    }

    boolean excepted = false;
    try {
      executor.execute(cycle);
    } catch (TaskExecutionException e) {
      excepted = true;
    }

    Assertions.assertTrue(excepted);
  }

  @Test
  @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
  public void emptyTest() {
    List<Task> tasks = List.of();
    Assertions.assertDoesNotThrow(() -> executor.execute(tasks));
  }

  @Test
  @Timeout(value = 10)
  public void nullDepsTest() {
    List<BasicTask> tasks = new ArrayList<>();
    AtomicInteger counter = new AtomicInteger();
    for (int i = 0; i != 100; ++i) {
      tasks.add(new BasicTask(
          counter::incrementAndGet,
          null
      ));
    }

    Assertions.assertDoesNotThrow(() -> executor.execute(tasks));
    Assertions.assertEquals(100, counter.get());
  }

  @Test
  @Timeout(value = 10)
  public void listOfNullDepsTest() {
    List<BasicTask> tasks = new ArrayList<>();
    AtomicInteger counter = new AtomicInteger();
    for (int i = 0; i != 100; ++i) {
      List<Task> deps = new ArrayList<>();
      for (int j = 0; j != i; ++j) {
        deps.add(null);
      }
      tasks.add(new BasicTask(
          counter::incrementAndGet,
          deps
      ));
    }

    Assertions.assertDoesNotThrow(() -> executor.execute(tasks));
    Assertions.assertEquals(100, counter.get());
  }

  @Test
  @Timeout(value = 10)
  public void notAllTasksListed() {
    List<BasicTask> tasks = new ArrayList<>();
    AtomicInteger counter = new AtomicInteger();
    for (int i = 0; i != 50; ++i) {
      Task dep = new BasicTask(counter::incrementAndGet, List.of());
      tasks.add(new BasicTask(
          counter::incrementAndGet,
          List.of(dep)
      ));
    }

    Assertions.assertDoesNotThrow(() -> executor.execute(tasks));
    Assertions.assertEquals(100, counter.get());
  }

  @Test
  @Timeout(value = 10)
  public void exceptionalTask() {
    List<BasicTask> tasks = List.of(new BasicTask(
        () -> { throw new RuntimeException("I'm exceptional"); },
        List.of())
    );
    boolean excepted = false;

    try {
      executor.execute(tasks);
    } catch (TaskExecutionException e) {
      excepted = true;
    }

    Assertions.assertTrue(excepted);
  }
}

package be.vlaanderen.omgeving.oddtoolkit.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import be.vlaanderen.omgeving.oddtoolkit.config.DefaultGeneratorRegistry;
import be.vlaanderen.omgeving.oddtoolkit.generator.BaseGenerator;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeneratorCliRunnerTest {

  @Test
  void executesAllRegisteredGeneratorsWhenAllIsRequested() throws Exception {
    DefaultGeneratorRegistry registry = new DefaultGeneratorRegistry();
    CountingGenerator sqlGenerator = new CountingGenerator("sql");
    CountingGenerator classDiagramGenerator = new CountingGenerator("class-diagram");
    registry.register(sqlGenerator.getName(), sqlGenerator);
    registry.register(classDiagramGenerator.getName(), classDiagramGenerator);

    GeneratorCliRunner runner = new GeneratorCliRunner(registry);
    runner.run("--generator=all");

    assertEquals(1, sqlGenerator.getExecutionCount());
    assertEquals(1, classDiagramGenerator.getExecutionCount());
  }

  @Test
  void executesOnlyRequestedGenerator() throws Exception {
    DefaultGeneratorRegistry registry = new DefaultGeneratorRegistry();
    CountingGenerator sqlGenerator = new CountingGenerator("sql");
    CountingGenerator classDiagramGenerator = new CountingGenerator("class-diagram");
    registry.register(sqlGenerator.getName(), sqlGenerator);
    registry.register(classDiagramGenerator.getName(), classDiagramGenerator);

    GeneratorCliRunner runner = new GeneratorCliRunner(registry);
    runner.run("--generator=sql");

    assertEquals(1, sqlGenerator.getExecutionCount());
    assertEquals(0, classDiagramGenerator.getExecutionCount());
  }

  @Test
  void throwsForUnknownGenerator() {
    DefaultGeneratorRegistry registry = new DefaultGeneratorRegistry();
    GeneratorCliRunner runner = new GeneratorCliRunner(registry);

    assertThrows(IllegalArgumentException.class, () -> runner.run("--generator=unknown"));
  }

  private static final class CountingGenerator extends BaseGenerator {

    private final String name;
    private int executionCount;

    private CountingGenerator(String name) {
      super(null, null, List.of());
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void generate() {
      executionCount++;
    }

    public int getExecutionCount() {
      return executionCount;
    }
  }
}


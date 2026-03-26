package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ClassDiagramGeneratorTest {
  private final ClassDiagramGenerator generator = TestGeneratorFactory.generator("class-diagram",
      ClassDiagramGenerator.class);

  @Test
  void testGetConcreteClasses() throws Exception {
    generator.run();
    String output = generator.getOutputFile();
    if (output != null) {
      Path p = Path.of(output);
      assertThat(Files.exists(p)).isTrue();
      String content = Files.readString(p);
      assertThat(content).contains("class");
    }
  }
}

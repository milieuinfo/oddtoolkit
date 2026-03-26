package be.vlaanderen.omgeving.oddtoolkit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class OddtoolkitApplicationTests {

  @Test
  void bootstrapLoads() {
    assertDoesNotThrow(() ->
        be.vlaanderen.omgeving.oddtoolkit.config.OddtoolkitBootstrap.bootstrap(
            new String[]{"--config-file=src/test/resources/application.yml"}));
  }
}

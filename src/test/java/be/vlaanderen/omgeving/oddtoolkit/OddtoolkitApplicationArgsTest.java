package be.vlaanderen.omgeving.oddtoolkit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class OddtoolkitApplicationArgsTest {

  @Test
  void bootstrapWithConfigFileCreatesRegistry() {
    var registry = be.vlaanderen.omgeving.oddtoolkit.config.OddtoolkitBootstrap.bootstrap(
        new String[]{"--config-file=src/test/resources/application.yml"});
    assertNotNull(registry);
  }

  @Test
  void bootstrapWithMissingFileFallsBackToDefaults() {
    var registry = be.vlaanderen.omgeving.oddtoolkit.config.OddtoolkitBootstrap.bootstrap(
        new String[]{"--config-file=missing.yml"});
    assertNotNull(registry);
  }
}

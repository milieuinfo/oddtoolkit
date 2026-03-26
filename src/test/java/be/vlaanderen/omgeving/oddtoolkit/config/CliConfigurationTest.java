package be.vlaanderen.omgeving.oddtoolkit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CliConfigurationTest {

  @Test
  void parsesAllGeneratorsSelection() {
    CliConfiguration configuration = CliConfiguration.fromArgs(new String[]{"--generator=all"});

    assertTrue(configuration.isValid());
    assertTrue(configuration.isAllGeneratorsRequested());
    assertEquals("all", configuration.getGeneratorName());
  }

  @Test
  void doesNotMarkNormalGeneratorAsAll() {
    CliConfiguration configuration = CliConfiguration.fromArgs(new String[]{"--generator=sql"});

    assertTrue(configuration.isValid());
    assertFalse(configuration.isAllGeneratorsRequested());
    assertEquals("sql", configuration.getGeneratorName());
  }
}


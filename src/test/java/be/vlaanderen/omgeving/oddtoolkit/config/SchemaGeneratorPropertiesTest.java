package be.vlaanderen.omgeving.oddtoolkit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SchemaGeneratorPropertiesTest {

  @Test
  void defaultJoinTableNamePatternKeepsLegacyRelPrefix() {
    SchemaGeneratorProperties properties = new SchemaGeneratorProperties();

    assertEquals("rel_{source_table}_{target_table}", properties.getJoinTableNamePattern());
  }

  @Test
  void excludedPairMatchesInBothDirections() {
    SchemaGeneratorProperties.ExcludedPair excludedPair = new SchemaGeneratorProperties.ExcludedPair();
    excludedPair.setSourceUri("urn:class:a");
    excludedPair.setTargetUri("urn:class:b");

    assertTrue(excludedPair.matches("urn:class:a", "urn:class:b"));
    assertTrue(excludedPair.matches("urn:class:b", "urn:class:a"));
  }

  @Test
  void excludedPairDoesNotMatchOtherUris() {
    SchemaGeneratorProperties.ExcludedPair excludedPair = new SchemaGeneratorProperties.ExcludedPair();
    excludedPair.setSourceUri("urn:class:a");
    excludedPair.setTargetUri("urn:class:b");

    assertFalse(excludedPair.matches("urn:class:a", "urn:class:c"));
  }
}

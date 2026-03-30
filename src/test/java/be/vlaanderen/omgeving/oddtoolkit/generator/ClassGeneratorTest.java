package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ClassGeneratorTest {
  private final ClassGenerator generator = TestGeneratorFactory.generator("class", ClassGenerator.class);

  @Test
  void testGetConcreteClasses() {
    generator.run();

    Map<String, Integer> extraIndex = new HashMap<>();
    List<be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration.ExtraProperty> extras =
        generator.getOntologyConfiguration().getExtraProperties();
    for (int i = 0; i < extras.size(); i++) {
      String uri = extras.get(i).getUri();
      if (uri != null) {
        extraIndex.putIfAbsent(uri, i);
      }
    }

    Map<String, Integer> temporalIndex = new HashMap<>();
    List<String> temporals = generator.getOntologyConfiguration().getTemporalProperties();
    for (int i = 0; i < temporals.size(); i++) {
      String uri = temporals.get(i);
      if (uri != null) {
        temporalIndex.putIfAbsent(uri, i);
      }
    }

    generator.getClasses().forEach(clazz -> {
      // Track the highest bucket seen so far: 0=pk, 1=extra, 2=temporal, 3=other
      int stage = 0;
      int lastExtra = -1;
      int lastTemporal = -1;
      for (ClassGenerator.Attribute attribute : clazz.getAttributes()) {
        String uri = attribute.getUri();
        if (attribute.isPrimaryKey()) {
          // PKs are always sorted first (bucket 0) — they must not appear after anything else.
          assertEquals(0, stage,
              "Primary key appears after extra/temporal/other attributes in " + clazz.getName());
          // If this PK is also an extra-property, advance stage to 1 so subsequent
          // pure extra-properties still satisfy the stage <= 1 guard.
          if (uri != null && extraIndex.containsKey(uri)) {
            stage = 1;
            int idx = extraIndex.get(uri);
            if (idx >= lastExtra) {
              lastExtra = idx;
            }
          }
          continue;
        }
        if (uri != null && extraIndex.containsKey(uri)) {
          assertTrue(stage <= 1,
              "Extra property appears after temporal/other in " + clazz.getName());
          stage = 1;
          int idx = extraIndex.get(uri);
          assertTrue(idx >= lastExtra,
              "Extra properties are not in config order in " + clazz.getName());
          lastExtra = idx;
          continue;
        }
        if (uri != null && temporalIndex.containsKey(uri)) {
          assertTrue(stage <= 2,
              "Temporal property appears after other attributes in " + clazz.getName());
          stage = 2;
          int idx = temporalIndex.get(uri);
          assertTrue(idx >= lastTemporal,
              "Temporal properties are not in config order in " + clazz.getName());
          lastTemporal = idx;
          continue;
        }
        stage = 3;
      }
    });
  }

  @Test
  void testEnumValueTrimming() {
    generator.run();

    ClassGenerator.Enum procedureEnum = generator.getEnums().stream()
        .filter(enumInfo -> "Procedure".equals(enumInfo.getName()))
        .findFirst()
        .orElse(null);

    assertNotNull(procedureEnum, "Expected Procedure enum to be extracted from ontology.enum-classes");

    List<String> enumValues = procedureEnum.getValues().stream()
        .map(ClassGenerator.EnumValue::getName)
        .toList();

    assertTrue(enumValues.contains("TRANSPORT"),
        "Expected TRANSPORT_PROCEDURE to be trimmed to TRANSPORT");
    assertFalse(enumValues.stream().anyMatch(value -> value.endsWith("_PROCEDURE")),
        "Expected redundant enum suffix token to be removed from Procedure enum values");
  }
}

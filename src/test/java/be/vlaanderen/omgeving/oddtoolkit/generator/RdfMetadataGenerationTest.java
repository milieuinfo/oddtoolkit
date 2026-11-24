package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

class RdfMetadataGenerationTest {
  private static final String CONFIG = "src/test/resources/examples/rdf-metadata/config.yml";
  private static final Path OUTPUT = Path.of("target/test-cache/rdf-metadata");

  @Test
  void generatesVersionSpecificRdfValueStorage() throws Exception {
    TestGeneratorFactory.generatorWithConfig("sql", SQLGenerator.class, CONFIG).run();
    TestGeneratorFactory.generatorWithConfig("java", JavaGenerator.class, CONFIG).run();
    TestGeneratorFactory.generatorWithConfig("typescript", TypescriptGenerator.class, CONFIG).run();
    TestGeneratorFactory.generatorWithConfig("er-diagram", ERDiagramGenerator.class, CONFIG).run();

    String sql = Files.readString(OUTPUT.resolve("schema.sql"));
    assertThat(sql).contains("CREATE TABLE resultaat_eigenschap (")
        .contains("object_kind VARCHAR NOT NULL")
        .contains("CHECK (object_kind IN ('IRI', 'LITERAL'))")
        .contains("datatype VARCHAR")
        .contains("language VARCHAR")
        .contains("ALTER TABLE resultaat_eigenschap ADD FOREIGN KEY (resultaat) REFERENCES resultaat(id)")
        .contains("ALTER TABLE observatie ADD FOREIGN KEY (has_result) REFERENCES resultaat(id)")
        .contains("ALTER TABLE rel_observatie_uitvoerder ADD FOREIGN KEY (observatie_uri) REFERENCES observatie(uri)")
        .contains("ALTER TABLE rel_observatie_uitvoerder ADD FOREIGN KEY (uitvoerder_uri) REFERENCES uitvoerder(uri)");

    String java = Files.readString(OUTPUT.resolve("java/ResultaatEigenschap.java"));
    assertThat(java).contains("private String predicate;")
        .contains("private String objectKind;")
        .contains("@ManyToOne")
        .contains("@JoinColumn(name = \"resultaat\", referencedColumnName = \"id\", nullable = false)");
    assertThat(Files.readString(OUTPUT.resolve("java/Resultaat.java")))
        .contains("@OneToMany(mappedBy = \"resultaat\")")
        .contains("private List<ResultaatEigenschap> rdfProperties;");
    assertThat(Files.readString(OUTPUT.resolve("java/Observatie.java")))
        .contains("joinColumns = @JoinColumn(name = \"observatie_uri\", referencedColumnName = \"uri\")")
        .contains("inverseJoinColumns = @JoinColumn(name = \"uitvoerder_uri\", referencedColumnName = \"uri\")");

    String typescript = Files.readString(OUTPUT.resolve("typescript/resultaateigenschap.model.ts"));
    assertThat(typescript).contains("predicate: string;")
        .contains("objectKind: string;")
        .contains("datatype?: string;")
        .contains("resultaat: Resultaat;");
    assertThat(Files.readString(OUTPUT.resolve("typescript/resultaat.model.ts")))
        .contains("rdfProperties?: ResultaatEigenschap[];");
    assertThat(Files.readString(OUTPUT.resolve("er.mmd")))
        .contains("resultaat_eigenschap {")
        .contains("resultaat_eigenschap many to one resultaat")
        .doesNotContain("observatie {");
  }

  @Test
  void generatedJavaSerializesEntriesWithoutOwnerCycle() throws Exception {
    TestGeneratorFactory.generatorWithConfig("java", JavaGenerator.class, CONFIG).run();
    Path classes = Files.createDirectories(OUTPUT.resolve("classes"));
    String classpath = String.join(File.pathSeparator, location(lombok.Getter.class),
        location(jakarta.persistence.Entity.class),
        location(com.fasterxml.jackson.annotation.JsonProperty.class),
        location(ObjectMapper.class), location(com.fasterxml.jackson.core.JsonGenerator.class));
    List<String> arguments = new ArrayList<>(List.of("--release", "17", "-proc:full",
        "-classpath", classpath, "-processorpath", location(lombok.Getter.class),
        "-d", classes.toString()));
    try (var files = Files.list(OUTPUT.resolve("java"))) {
      files.filter(path -> path.toString().endsWith(".java"))
          .map(Path::toString).forEach(arguments::add);
    }
    ByteArrayOutputStream errors = new ByteArrayOutputStream();
    assertThat(ToolProvider.getSystemJavaCompiler().run(null, null, errors,
        arguments.toArray(String[]::new))).as("javac: %s", errors).isZero();
    try (URLClassLoader loader = new URLClassLoader(new URL[] { classes.toUri().toURL() },
        getClass().getClassLoader())) {
      Class<?> ownerType = loader.loadClass("be.vlaanderen.omgeving.oddtoolkit.generated.Resultaat");
      Class<?> entryType = loader.loadClass("be.vlaanderen.omgeving.oddtoolkit.generated.ResultaatEigenschap");
      Object owner = ownerType.getConstructor().newInstance();
      Object entry = entryType.getConstructor().newInstance();
      entryType.getMethod("setPredicate", String.class).invoke(entry, "https://example.org/p");
      entryType.getMethod("setValue", String.class).invoke(entry, "value");
      entryType.getMethod("setObjectKind", String.class).invoke(entry, "LITERAL");
      entryType.getMethod("setResultaat", ownerType).invoke(entry, owner);
      ownerType.getMethod("setRdfProperties", List.class).invoke(owner, List.of(entry));
      var json = new ObjectMapper().valueToTree(owner);
      assertThat(json.get("rdfProperties").get(0).get("predicate").asText())
          .isEqualTo("https://example.org/p");
      assertThat(json.get("rdfProperties").get(0).has("resultaat")).isFalse();
    }
  }

  @Test
  void joinForeignKeyReferencesTheWholeCompositeKey() throws Exception {
    TestGeneratorFactory.generatorWithConfig("sql", SQLGenerator.class,
        "src/test/resources/examples/rdf-metadata/composite-config.yml").run();
    assertThat(Files.readString(OUTPUT.resolve("composite.sql")))
        .contains("ALTER TABLE rel_resultaat_uitvoerder ADD FOREIGN KEY "
            + "(resultaat_local_id, resultaat_version) REFERENCES resultaat(local_id, version)");
  }

  @Test
  void rejectsRdfMetadataWhenOwnerHasCompositeVersionKey() {
    SQLGenerator generator = TestGeneratorFactory.generatorWithConfig("sql", SQLGenerator.class, CONFIG);
    generator.getOntologyConfiguration().getSurrogateKeys().setEnabled(false);
    try {
      assertThatThrownBy(generator::run)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("must have one version-specific primary key");
    } finally {
      generator.getOntologyConfiguration().getSurrogateKeys().setEnabled(true);
    }
  }

  @Test
  void usesConceptNamesForOntologyAndRdfMetadataFields() throws Exception {
    String config = "src/test/resources/examples/rdf-metadata/dutch-config.yml";
    TestGeneratorFactory.generatorWithConfig("sql", SQLGenerator.class, config).run();
    TestGeneratorFactory.generatorWithConfig("java", JavaGenerator.class, config).run();
    TestGeneratorFactory.generatorWithConfig("typescript", TypescriptGenerator.class, config).run();

    Path output = OUTPUT.resolve("nl");
    String sql = Files.readString(output.resolve("schema.sql"));
    assertThat(sql).contains("waargenomen_met VARCHAR")
        .contains("verschijnseltijd VARCHAR")
        .contains("predicaat VARCHAR NOT NULL")
        .contains("objectwaarde VARCHAR NOT NULL")
        .contains("objectsoort VARCHAR NOT NULL")
        .contains("taal VARCHAR")
        .contains("CHECK (objectsoort IN ('IRI', 'LITERAL'))")
        .contains("taal IS NULL")
        .doesNotContain("object_kind");
    assertThat(Files.readString(output.resolve("java/ResultaatEigenschap.java")))
        .contains("private String predicaat;")
        .contains("private String objectwaarde;")
        .contains("private String objectsoort;")
        .contains("@JsonProperty(\"taal\")");
    assertThat(Files.readString(output.resolve("java/Resultaat.java")))
        .contains("private List<ResultaatEigenschap> resultaatEigenschappen;");
    assertThat(Files.readString(output.resolve("typescript/observatie.model.ts")))
        .contains("waargenomenMet?: Sensor;")
        .contains("verschijnseltijd?: string;");
    assertThat(Files.readString(output.resolve("typescript/resultaat.model.ts")))
        .contains("resultaatEigenschappen?: ResultaatEigenschap[];");
  }

  private static String location(Class<?> type) throws Exception {
    return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
  }
}

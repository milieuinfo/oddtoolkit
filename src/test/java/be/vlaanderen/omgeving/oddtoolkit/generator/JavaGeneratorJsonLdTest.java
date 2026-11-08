package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.assertj.core.api.Assertions.assertThat;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The JSON-LD options of the Java generator ({@code json-ld: true}, {@code date-time-type: offset})
 * on the RIE-IEPR test ontology: gaps G1-G5 of feature 004.
 */
class JavaGeneratorJsonLdTest {

  private static final Path CONFIG = Path.of("target/test-cache/application-jsonld.yml");
  private static final Path OUTPUT = Path.of("target/test-cache/java-jsonld");
  private static final String PACKAGE = "be.vlaanderen.omgeving.riepr.model.structuur";
  private static final String RIEPR = "https://data.riepr.omgeving.vlaanderen.be/ns/riepr#";

  @BeforeAll
  static void generate() throws Exception {
    writeJsonLdConfig();
    TestGeneratorFactory.generatorWithConfig("java", JavaGenerator.class, CONFIG.toString()).run();
  }

  /** Standard test configuration plus the JSON-LD options, surrogate keys and WKT as text (like RIE-IEPR). */
  @SuppressWarnings("unchecked")
  private static void writeJsonLdConfig() throws IOException {
    ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
    Map<String, Object> root = yaml.readValue(new File("src/test/resources/application.yml"), Map.class);
    Map<String, Object> ontology = (Map<String, Object>) root.get("ontology");
    ontology.put("surrogate-keys", new LinkedHashMap<>(Map.of("enabled", true, "name", "id")));
    List<Object> overrides = new ArrayList<>((List<Object>) ontology.get("override-datatypes"));
    overrides.add(Map.of("uri", "http://www.opengis.net/ont/geosparql#wktLiteral",
        "override", "http://www.w3.org/2001/XMLSchema#string"));
    ontology.put("override-datatypes", overrides);
    Map<String, Object> java = (Map<String, Object>) ((Map<String, Object>) root.get("generators"))
        .get("java-generator");
    java.put("output-directory", OUTPUT.toString());
    java.put("json-ld", true);
    java.put("date-time-type", "offset");
    Files.createDirectories(CONFIG.getParent());
    yaml.writeValue(CONFIG.toFile(), root);
  }

  private static String source(String className) throws IOException {
    return Files.readString(OUTPUT.resolve(className + ".java"));
  }

  @Test
  void g1DateTimeIsOffsetDateTime() throws Exception {
    String exploitatie = source("Exploitatie");
    assertThat(exploitatie).contains("import java.time.OffsetDateTime;")
        .contains("private OffsetDateTime aangemaaktOp;")
        .doesNotContain("LocalDateTime");
  }

  @Test
  void g2ClassesSerializeTheirRdfType() throws Exception {
    assertThat(source("Exploitatie"))
        .contains("@JsonProperty(value = \"@type\", access = JsonProperty.Access.READ_ONLY)")
        .contains("public String getRdfType() {")
        .contains("return \"" + RIEPR + "Exploitatie\";");
    assertThat(source("ExterneIdentificator"))
        .contains("return \"http://www.w3.org/ns/adms#Identifier\";");
    assertThat(source("ISysteem")).doesNotContain("getRdfType");
    assertThat(source("Status")).doesNotContain("getRdfType");
  }

  @Test
  void g3DatatypeCompanionSerializesOneTypedLiteral() throws Exception {
    String identificator = source("ExterneIdentificator");
    assertThat(count(identificator, "@JsonProperty(\"notation\")")).isEqualTo(1);
    assertThat(fieldBlock(identificator, "notatie")).contains("@JsonIgnore");
    assertThat(fieldBlock(identificator, "notatie_datatype")).contains("@JsonIgnore");
    assertThat(identificator).contains("public Object getNotatieJsonLd() {")
        .contains("@JsonSetter(\"notation\")")
        .contains("public void setNotatieJsonLd(Object value) {");
  }

  @Test
  void g4EntityReferencesSerializeAsIri() throws Exception {
    String exploitatie = source("Exploitatie");
    assertThat(fieldBlock(exploitatie, "locatie"))
        .contains("@JsonSerialize(using = IriReferentieSerializer.class)");
    assertThat(fieldBlock(exploitatie, "systemen"))
        .contains("@JsonSerialize(contentUsing = IriReferentieSerializer.class)");
    // Helper objects without URI template stay nested.
    assertThat(fieldBlock(exploitatie, "identifier")).doesNotContain("@JsonSerialize");
    String locatie = source("Exploitatielocatie");
    assertThat(fieldBlock(locatie, "adres")).doesNotContain("@JsonSerialize");
    assertThat(fieldBlock(locatie, "geometrie")).doesNotContain("@JsonSerialize");
    assertThat(exploitatie).contains("public class Exploitatie implements IriReferentie {");
    assertThat(OUTPUT.resolve("IriReferentie.java")).exists();
    assertThat(OUTPUT.resolve("IriReferentieSerializer.java")).exists();
  }

  @Test
  void g5EnumsSerializeAsConceptUri() throws Exception {
    assertThat(source("Status")).contains("@JsonValue")
        .contains("@JsonCreator")
        .contains("public static Status fromUri(String uri) {");
  }

  /** Compiles the generated sources (Lombok included) and serializes an object graph with Jackson. */
  @Test
  void generatedPojosSerializeToJsonLd() throws Exception {
    ClassLoader loader = compileGeneratedSources();
    ObjectMapper mapper = new ObjectMapper().registerModule(offsetDateTimeAsIso());

    Object locatie = instance(loader, "Exploitatielocatie");
    set(locatie, "setUri", "https://data.mjv.omgeving.vlaanderen.be/id/exploitatielocatie/l1");
    Object installatie = instance(loader, "Installatie");
    set(installatie, "setUri", "https://data.mjv.omgeving.vlaanderen.be/id/installatie/i1");
    Object identificator = instance(loader, "ExterneIdentificator");
    set(identificator, "setNotatie", "0123456789");
    set(identificator, "setNotatie_datatype", "https://example.org/KBO");
    set(identificator, "setSchemaAgency", "KBO");
    Class<?> statusType = loader.loadClass(PACKAGE + ".Status");
    Object status = statusType.getMethod("fromUri", String.class).invoke(null,
        "https://data.omgeving.vlaanderen.be/id/concept/riepr/status-type/in_dienst");

    Object exploitatie = instance(loader, "Exploitatie");
    set(exploitatie, "setUri", "https://data.mjv.omgeving.vlaanderen.be/id/exploitatie/e1");
    set(exploitatie, "setAangemaaktOp", OffsetDateTime.parse("2026-07-01T10:00:00Z"));
    set(exploitatie, "setLocatie", locatie);
    set(exploitatie, "setSystemen", List.of(installatie));
    set(exploitatie, "setIdentifier", List.of(identificator));
    set(exploitatie, "setStatus", status);

    String json = mapper.writeValueAsString(exploitatie);
    JsonNode node = mapper.readTree(json);
    assertThat(node.get("created").asText()).isEqualTo("2026-07-01T10:00:00Z");
    assertThat(node.get("@type").asText()).isEqualTo(RIEPR + "Exploitatie");
    assertThat(node.get("deployedOnPlatform").asText())
        .isEqualTo("https://data.mjv.omgeving.vlaanderen.be/id/exploitatielocatie/l1");
    assertThat(node.get("deployedSystem").get(0).asText())
        .isEqualTo("https://data.mjv.omgeving.vlaanderen.be/id/installatie/i1");
    assertThat(node.get("status").asText())
        .isEqualTo("https://data.omgeving.vlaanderen.be/id/concept/riepr/status-type/in_dienst");
    JsonNode identifier = node.get("identifier").get(0);
    assertThat(identifier.get("@type").asText()).isEqualTo("http://www.w3.org/ns/adms#Identifier");
    assertThat(identifier.get("notation").get("@value").asText()).isEqualTo("0123456789");
    assertThat(identifier.get("notation").get("@type").asText()).isEqualTo("https://example.org/KBO");
    assertThat(identifier.get("schemaAgency").asText()).isEqualTo("KBO");
    assertThat(count(json, "\"notation\"")).isEqualTo(1);

    // Reading back: @type is ignored, a typed literal fills value and datatype, a concept URI the enum.
    Class<?> identificatorType = loader.loadClass(PACKAGE + ".ExterneIdentificator");
    Object gelezen = mapper.readValue(
        "{\"@type\":\"http://www.w3.org/ns/adms#Identifier\",\"notation\":"
            + "{\"@value\":\"0123456789\",\"@type\":\"https://example.org/KBO\"}}",
        identificatorType);
    assertThat(identificatorType.getMethod("getNotatie").invoke(gelezen)).isEqualTo("0123456789");
    assertThat(identificatorType.getMethod("getNotatie_datatype").invoke(gelezen))
        .isEqualTo("https://example.org/KBO");
    Object plain = mapper.readValue("{\"notation\":\"0123\"}", identificatorType);
    assertThat(identificatorType.getMethod("getNotatie").invoke(plain)).isEqualTo("0123");
    assertThat(identificatorType.getMethod("getNotatie_datatype").invoke(plain)).isNull();
    assertThat(mapper.readValue(
        "\"https://data.omgeving.vlaanderen.be/id/concept/riepr/status-type/in_dienst\"", statusType))
        .isSameAs(status);
  }

  private static ClassLoader compileGeneratedSources() throws Exception {
    Path classes = Files.createTempDirectory("oddtoolkit-jsonld-classes");
    List<String> sources;
    try (Stream<Path> files = Files.list(OUTPUT)) {
      sources = files.filter(p -> p.toString().endsWith(".java")).map(Path::toString).sorted().toList();
    }
    String lombok = location(lombok.Getter.class);
    String classpath = String.join(File.pathSeparator, lombok,
        location(jakarta.persistence.Entity.class),
        location(com.fasterxml.jackson.annotation.JsonProperty.class),
        location(com.fasterxml.jackson.databind.ObjectMapper.class),
        location(com.fasterxml.jackson.core.JsonGenerator.class));
    List<String> args = new ArrayList<>(List.of("--release", "17", "-proc:full",
        "-classpath", classpath, "-processorpath", lombok, "-d", classes.toString()));
    args.addAll(sources);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    java.io.ByteArrayOutputStream errors = new java.io.ByteArrayOutputStream();
    int result = compiler.run(null, null, errors, args.toArray(String[]::new));
    assertThat(result).as("javac output:%n%s", errors).isZero();
    return new URLClassLoader(new URL[]{classes.toUri().toURL()},
        JavaGeneratorJsonLdTest.class.getClassLoader());
  }

  private static String location(Class<?> type) throws Exception {
    return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
  }

  private static Object instance(ClassLoader loader, String simpleName) throws Exception {
    return loader.loadClass(PACKAGE + "." + simpleName).getConstructor().newInstance();
  }

  private static void set(Object target, String setter, Object value) throws Exception {
    for (Method method : target.getClass().getMethods()) {
      if (method.getName().equals(setter) && method.getParameterCount() == 1) {
        method.invoke(target, value);
        return;
      }
    }
    throw new AssertionError("No setter " + setter + " on " + target.getClass());
  }

  // The test classpath has no jackson-datatype-jsr310; the consumer registers JavaTimeModule.
  private static SimpleModule offsetDateTimeAsIso() {
    return new SimpleModule().addSerializer(new StdSerializer<>(OffsetDateTime.class) {
      @Override
      public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider provider)
          throws IOException {
        gen.writeString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value));
      }
    });
  }

  /** The annotations and declaration of a field, up to and including its ';'. */
  private static String fieldBlock(String source, String fieldName) {
    Matcher matcher = Pattern.compile("\\*/\\n((?:\\t@[^\\n]*\\n|\\t\\t[^\\n]*\\n|\\t\\)\\n)*)"
        + "\\tprivate [^\\n]* " + Pattern.quote(fieldName) + ";").matcher(source);
    assertThat(matcher.find()).as("field '%s' should be present", fieldName).isTrue();
    return matcher.group();
  }

  private static int count(String haystack, String needle) {
    int count = 0;
    for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
      count++;
    }
    return count;
  }
}

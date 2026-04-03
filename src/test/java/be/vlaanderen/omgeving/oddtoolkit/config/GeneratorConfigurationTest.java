package be.vlaanderen.omgeving.oddtoolkit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.generator.BaseGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.JavaGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.TypescriptGenerator;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeneratorConfigurationTest {

  @Test
  void javaAndTypescriptGeneratorsUseTheirOwnAdapterConfiguration() throws Exception {
    OntologyInfo ontologyInfo = new OntologyInfo(new OntologyConfiguration());

    MarkerAdapter javaAdapter = new MarkerAdapter();
    MarkerAdapter typescriptAdapter = new MarkerAdapter();
    MarkerAdapter shaclAdapter = new MarkerAdapter();

    Map<String, AbstractAdapter<?>> adapterBeans = new LinkedHashMap<>();
    adapterBeans.put("java-adapter", javaAdapter);
    adapterBeans.put("typescript-adapter", typescriptAdapter);
    adapterBeans.put("shacl-adapter", shaclAdapter);

    GeneratorProperties generatorProperties = new GeneratorProperties();
    generatorProperties.setGenerators(Map.of(
        "java", Map.of("adapters", List.of("java-adapter")),
        "typescript", Map.of("adapters", List.of("typescript-adapter")),
        "shacl", Map.of("adapters", List.of("shacl-adapter"))));

    GeneratorConfiguration configuration = new GeneratorConfiguration();
    JavaGenerator javaGenerator = configuration.javaGenerator(
        ontologyInfo,
        generatorProperties,
        new DiagramGeneratorProperties(),
        new SchemaGeneratorProperties(),
        new JavaGeneratorProperties(),
        adapterBeans);

    TypescriptGenerator typescriptGenerator = configuration.typescriptGenerator(
        ontologyInfo,
        generatorProperties,
        new TypescriptGeneratorProperties(),
        adapterBeans);

    assertEquals(List.of(javaAdapter), adaptersOf(javaGenerator));
    assertEquals(List.of(typescriptAdapter), adaptersOf(typescriptGenerator));
  }

  @SuppressWarnings("unchecked")
  private static List<AbstractAdapter<?>> adaptersOf(BaseGenerator generator) throws Exception {
    Field adaptersField = BaseGenerator.class.getDeclaredField("adapters");
    adaptersField.setAccessible(true);
    return (List<AbstractAdapter<?>>) adaptersField.get(generator);
  }

  private static final class MarkerAdapter extends AbstractAdapter<OntologyInfo> {
    private MarkerAdapter() {
      super(OntologyInfo.class, false);
    }

    @Override
    public OntologyInfo adapt(OntologyInfo info) {
      return info;
    }
  }
}


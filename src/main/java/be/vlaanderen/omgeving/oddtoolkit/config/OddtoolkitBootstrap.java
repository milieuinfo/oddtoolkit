package be.vlaanderen.omgeving.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.ConceptClassExtractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.ConceptSchemeExtractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.ConceptSchemeLoadAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.OntologyClassExtractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.OntologyDatatypeOverrideAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.OntologyExtractExternalAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.OntologyExtractIndividualsAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.OntologyLoadAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.OntologyPropertyExtraAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.OntologyPropertyExtractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.OntologyPropertyOverrideAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.OntologyReasonerAdapter;
import be.vlaanderen.omgeving.oddtoolkit.adapter.OntologyUriTemplateAdapter;
import be.vlaanderen.omgeving.oddtoolkit.generator.BikeshedGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ClassDiagramGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ClassGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.DataFrameGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ERDiagramGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.JavaGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.SQLGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ShaclGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.TypescriptGenerator;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OddtoolkitBootstrap {

  private OddtoolkitBootstrap() {
  }

  public static GeneratorRegistry bootstrap(String[] args) {
    CliConfiguration cliConfiguration = CliConfiguration.fromArgs(args);
    Map<String, Object> rootConfiguration = ConfigurationSourceResolver
        .loadFromFile(cliConfiguration.getConfigFile());

    OntologyConfiguration ontologyConfiguration = bindAnnotated(rootConfiguration,
        OntologyConfiguration.class, new OntologyConfiguration());
    OntologyReasonerProperties ontologyReasonerProperties = bindAnnotated(rootConfiguration,
        OntologyReasonerProperties.class, new OntologyReasonerProperties());
    OntologyExtractExternalAdapter.ExtractExternalProperties extractExternalProperties =
        bindAnnotated(rootConfiguration,
            OntologyExtractExternalAdapter.ExtractExternalProperties.class,
            new OntologyExtractExternalAdapter.ExtractExternalProperties());

    applyCliOverrides(ontologyConfiguration, cliConfiguration);

    GeneratorProperties generatorProperties = new GeneratorProperties();
    generatorProperties.setGenerators(readGeneratorMap(rootConfiguration));

    ClassDiagramProperties classDiagramProperties = bindAnnotated(rootConfiguration,
        ClassDiagramProperties.class, new ClassDiagramProperties());
    DiagramGeneratorProperties diagramGeneratorProperties = bindAnnotated(rootConfiguration,
        DiagramGeneratorProperties.class, new DiagramGeneratorProperties());
    DataFrameGeneratorProperties dataFrameGeneratorProperties = bindAnnotated(rootConfiguration,
        DataFrameGeneratorProperties.class, new DataFrameGeneratorProperties());
    ERDiagramProperties erDiagramProperties = bindAnnotated(rootConfiguration,
        ERDiagramProperties.class, new ERDiagramProperties());
    JavaGeneratorProperties javaGeneratorProperties = bindAnnotated(rootConfiguration,
        JavaGeneratorProperties.class, new JavaGeneratorProperties());
    SQLGeneratorProperties sqlGeneratorProperties = bindAnnotated(rootConfiguration,
        SQLGeneratorProperties.class, new SQLGeneratorProperties());
    SchemaGeneratorProperties schemaGeneratorProperties = bindAnnotated(rootConfiguration,
        SchemaGeneratorProperties.class, new SchemaGeneratorProperties());
    ShaclGeneratorProperties shaclGeneratorProperties = bindAnnotated(rootConfiguration,
        ShaclGeneratorProperties.class, new ShaclGeneratorProperties());
    TypescriptGeneratorProperties typescriptGeneratorProperties = bindAnnotated(rootConfiguration,
        TypescriptGeneratorProperties.class, new TypescriptGeneratorProperties());
    BikeshedGeneratorProperties bikeshedGeneratorProperties = bindAnnotated(rootConfiguration,
        BikeshedGeneratorProperties.class, new BikeshedGeneratorProperties());

    OntologyInfo ontologyInfo = new OntologyInfo(ontologyConfiguration);
    Map<String, AbstractAdapter<?>> adapterBeans = createAdapterBeans(rootConfiguration, ontologyInfo,
        ontologyConfiguration, ontologyReasonerProperties, extractExternalProperties);

    GeneratorConfiguration generatorConfiguration = new GeneratorConfiguration();

    DataFrameGenerator dataFrameGenerator = generatorConfiguration.dataFrameGenerator(ontologyInfo,
        generatorProperties, dataFrameGeneratorProperties, adapterBeans);
    ClassGenerator classGenerator = generatorConfiguration.classGenerator(ontologyInfo,
        generatorProperties, adapterBeans);
    ClassDiagramGenerator classDiagramGenerator = generatorConfiguration.classDiagramGenerator(
        ontologyInfo, generatorProperties, classDiagramProperties, diagramGeneratorProperties,
        adapterBeans);
    ERDiagramGenerator erDiagramGenerator = generatorConfiguration.erDiagramGenerator(ontologyInfo,
        generatorProperties, diagramGeneratorProperties, schemaGeneratorProperties,
        erDiagramProperties, adapterBeans);
    SQLGenerator sqlGenerator = generatorConfiguration.sqlGenerator(ontologyInfo,
        generatorProperties, diagramGeneratorProperties, schemaGeneratorProperties,
        sqlGeneratorProperties, adapterBeans);
    ShaclGenerator shaclGenerator = generatorConfiguration.shaclGenerator(ontologyInfo,
        generatorProperties, shaclGeneratorProperties, adapterBeans);
    JavaGenerator javaGenerator = generatorConfiguration.javaGenerator(ontologyInfo,
        generatorProperties, diagramGeneratorProperties, schemaGeneratorProperties,
        javaGeneratorProperties, adapterBeans);
    TypescriptGenerator typescriptGenerator = generatorConfiguration.typescriptGenerator(ontologyInfo,
        generatorProperties, typescriptGeneratorProperties, adapterBeans);
    BikeshedGenerator bikeshedGenerator = generatorConfiguration.bikeshedGenerator(ontologyInfo,
        generatorProperties, bikeshedGeneratorProperties, adapterBeans);

    DefaultGeneratorRegistry registry = new DefaultGeneratorRegistry();
    registry.register(dataFrameGenerator.getName(), dataFrameGenerator);
    registry.register(classGenerator.getName(), classGenerator);
    registry.register(classDiagramGenerator.getName(), classDiagramGenerator);
    registry.register(erDiagramGenerator.getName(), erDiagramGenerator);
    registry.register(sqlGenerator.getName(), sqlGenerator);
    registry.register(shaclGenerator.getName(), shaclGenerator);
    registry.register(javaGenerator.getName(), javaGenerator);
    registry.register(typescriptGenerator.getName(), typescriptGenerator);
    registry.register(bikeshedGenerator.getName(), bikeshedGenerator);
    return registry;
  }

  private static <T> T bindAnnotated(Map<String, Object> root, Class<T> type, T defaultValue) {
    return ConfigurationBinder.bind(root, type, defaultValue);
  }

  private static void applyCliOverrides(OntologyConfiguration ontologyConfiguration,
      CliConfiguration cliConfiguration) {
    if (cliConfiguration.getOntologyFilePath() != null
        && !cliConfiguration.getOntologyFilePath().isBlank()) {
      ontologyConfiguration.setOntologyFilePath(cliConfiguration.getOntologyFilePath().trim());
    }
    if (cliConfiguration.getConceptsFilePath() != null
        && !cliConfiguration.getConceptsFilePath().isBlank()) {
      ontologyConfiguration.setConceptsFilePath(cliConfiguration.getConceptsFilePath().trim());
    }
  }

  private static Map<String, Map<String, Object>> readGeneratorMap(Map<String, Object> root) {
    Object generators = root.get("generators");
    if (!(generators instanceof Map<?, ?> generatorMap)) {
      return new LinkedHashMap<>();
    }

    Map<String, Map<String, Object>> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : generatorMap.entrySet()) {
      if (entry.getKey() == null || !(entry.getValue() instanceof Map<?, ?> section)) {
        continue;
      }
      result.put(entry.getKey().toString(), toStringObjectMap(section));
    }
    return result;
  }

  private static Map<String, AbstractAdapter<?>> createAdapterBeans(Map<String, Object> root,
      OntologyInfo ontologyInfo,
      OntologyConfiguration ontologyConfiguration,
      OntologyReasonerProperties reasonerProperties,
      OntologyExtractExternalAdapter.ExtractExternalProperties externalProperties) {
    Map<String, AbstractAdapter<?>> allAdapters = new LinkedHashMap<>();
    allAdapters.put("ontology-load", new OntologyLoadAdapter());
    allAdapters.put("ontology-extract-external", new OntologyExtractExternalAdapter(externalProperties));
    allAdapters.put("ontology-reasoner",
        new OntologyReasonerAdapter(reasonerProperties, ontologyConfiguration));
    allAdapters.put("ontology-class-extract", new OntologyClassExtractAdapter());
    allAdapters.put("ontology-uri-template", new OntologyUriTemplateAdapter());
    allAdapters.put("ontology-property-extract", new OntologyPropertyExtractAdapter());
    allAdapters.put("ontology-property-override",
        new OntologyPropertyOverrideAdapter(ontologyConfiguration));
    allAdapters.put("ontology-property-extra", new OntologyPropertyExtraAdapter(ontologyConfiguration));
    allAdapters.put("ontology-datatype-override",
        new OntologyDatatypeOverrideAdapter(ontologyConfiguration));
    allAdapters.put("ontology-individuals-extract", new OntologyExtractIndividualsAdapter());
    allAdapters.put("concept-scheme-load", new ConceptSchemeLoadAdapter());
    allAdapters.put("concept-scheme-extract", new ConceptSchemeExtractAdapter());
    allAdapters.put("concept-class-extract", new ConceptClassExtractAdapter(ontologyInfo));

    Map<String, AbstractAdapter<?>> enabledAdapters = new LinkedHashMap<>();
    for (Map.Entry<String, AbstractAdapter<?>> entry : allAdapters.entrySet()) {
      if (adapterEnabled(root, entry.getKey(), entry.getValue().getClass())) {
        enabledAdapters.put(entry.getKey(), entry.getValue());
      }
    }
    return enabledAdapters;
  }

  private static boolean adapterEnabled(Map<String, Object> root, String adapterName,
      Class<?> adapterType) {
    ConditionalOnConfigProperty conditional = adapterType.getAnnotation(
        ConditionalOnConfigProperty.class);
    if (conditional != null) {
      String path = conditional.prefix() + "." + conditional.name();
      Object rawValue = resolvePathValue(root, path);
      if (rawValue == null) {
        return conditional.matchIfMissing();
      }
      return conditional.havingValue().equalsIgnoreCase(rawValue.toString());
    }

    // Fallback for adapters without explicit annotation.
    Object enabled = resolvePathValue(root, "adapters." + adapterName + ".enabled");
    if (enabled == null) {
      return true;
    }
    if (enabled instanceof Boolean value) {
      return value;
    }
    return Boolean.parseBoolean(enabled.toString());
  }

  private static Object resolvePathValue(Map<String, Object> root, String dotPath) {
    Object current = root;
    for (String token : dotPath.split("\\.")) {
      if (!(current instanceof Map<?, ?> map)) {
        return null;
      }
      current = map.get(token);
      if (current == null) {
        return null;
      }
    }
    return current;
  }

  private static Map<String, Object> toStringObjectMap(Map<?, ?> source) {
    Map<String, Object> out = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : source.entrySet()) {
      if (entry.getKey() == null) {
        continue;
      }
      out.put(entry.getKey().toString(), normalizeValue(entry.getValue()));
    }
    return out;
  }

  private static Object normalizeValue(Object value) {
    if (value instanceof Map<?, ?> nestedMap) {
      return toStringObjectMap(nestedMap);
    }
    if (value instanceof List<?> list) {
      List<Object> normalized = new ArrayList<>();
      for (Object item : list) {
        normalized.add(normalizeValue(item));
      }
      return normalized;
    }
    return value;
  }
}


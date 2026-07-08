package be.vlaanderen.omgeving.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.generator.BikeshedGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ClassDiagramGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ClassGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.DataFrameGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ERDiagramGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.JavaGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ODCSGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.SQLGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.ShaclGenerator;
import be.vlaanderen.omgeving.oddtoolkit.generator.TypescriptGenerator;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating generator instances and filtering adapters based on configuration.
 */
public class GeneratorConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(GeneratorConfiguration.class);

  public DataFrameGenerator dataFrameGenerator(OntologyInfo ontologyInfo,
      GeneratorProperties generatorProperties,
      DataFrameGeneratorProperties dataFrameGeneratorProperties,
      Map<String, AbstractAdapter<?>> adapterBeans) {
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("class"));
    return new DataFrameGenerator(ontologyInfo, ontologyInfo.getConcepts(), adapters,
        dataFrameGeneratorProperties);
  }

  public ClassGenerator classGenerator(OntologyInfo ontologyInfo,
      GeneratorProperties generatorProperties,
      Map<String, AbstractAdapter<?>> adapterBeans) {
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("class"));
    return new ClassGenerator(ontologyInfo, ontologyInfo.getConcepts(), adapters);
  }

  public ClassDiagramGenerator classDiagramGenerator(OntologyInfo ontologyInfo,
      GeneratorProperties generatorProperties,
      ClassDiagramProperties classDiagramProperties,
      DiagramGeneratorProperties diagramGeneratorProperties,
      Map<String, AbstractAdapter<?>> adapterBeans) {
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans,
        generatorProperties.adaptersFor("class-diagram"));
    return new ClassDiagramGenerator(ontologyInfo, ontologyInfo.getConcepts(), adapters,
        classDiagramProperties, diagramGeneratorProperties);
  }

  public ERDiagramGenerator erDiagramGenerator(OntologyInfo ontologyInfo,
      GeneratorProperties generatorProperties,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      ERDiagramProperties erDiagramProperties,
      Map<String, AbstractAdapter<?>> adapterBeans) {
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans,
        generatorProperties.adaptersFor("er-diagram"));
    return new ERDiagramGenerator(ontologyInfo, ontologyInfo.getConcepts(), adapters,
        diagramGeneratorProperties, schemaGeneratorProperties, erDiagramProperties);
  }

  public SQLGenerator sqlGenerator(OntologyInfo ontologyInfo,
      GeneratorProperties generatorProperties,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      SQLGeneratorProperties sqlGeneratorProperties,
      Map<String, AbstractAdapter<?>> adapterBeans) {
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("sql"));
    return new SQLGenerator(ontologyInfo, ontologyInfo.getConcepts(), adapters,
        diagramGeneratorProperties, schemaGeneratorProperties, sqlGeneratorProperties);
  }

  public ShaclGenerator shaclGenerator(OntologyInfo ontologyInfo,
      GeneratorProperties generatorProperties,
      ShaclGeneratorProperties shaclGeneratorProperties,
      Map<String, AbstractAdapter<?>> adapterBeans) {
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("shacl"));
    return new ShaclGenerator(ontologyInfo, ontologyInfo.getConcepts(), adapters,
        shaclGeneratorProperties);
  }

  public JavaGenerator javaGenerator(OntologyInfo ontologyInfo,
      GeneratorProperties generatorProperties,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      JavaGeneratorProperties javaGeneratorProperties,
      Map<String, AbstractAdapter<?>> adapterBeans) {
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("java"));
    return new JavaGenerator(ontologyInfo, ontologyInfo.getConcepts(), adapters,
        diagramGeneratorProperties, schemaGeneratorProperties, javaGeneratorProperties);
  }

  public TypescriptGenerator typescriptGenerator(OntologyInfo ontologyInfo,
      GeneratorProperties generatorProperties,
      TypescriptGeneratorProperties typescriptGeneratorProperties,
      Map<String, AbstractAdapter<?>> adapterBeans) {
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("typescript"));
    return new TypescriptGenerator(ontologyInfo, ontologyInfo.getConcepts(), adapters,
        typescriptGeneratorProperties);
  }

  public BikeshedGenerator bikeshedGenerator(OntologyInfo ontologyInfo,
      GeneratorProperties generatorProperties,
      BikeshedGeneratorProperties bikeshedGeneratorProperties,
      Map<String, AbstractAdapter<?>> adapterBeans) {
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans,
        generatorProperties.adaptersFor("bikeshed"));
    return new BikeshedGenerator(ontologyInfo, ontologyInfo.getConcepts(), adapters,
        bikeshedGeneratorProperties);
  }

  public ODCSGenerator odcsGenerator(OntologyInfo ontologyInfo,
      GeneratorProperties generatorProperties,
      ODCSGeneratorProperties odcsGeneratorProperties,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      Map<String, AbstractAdapter<?>> adapterBeans) {
    List<AbstractAdapter<?>> adapters = selectAdapters(adapterBeans, generatorProperties.adaptersFor("odcs"));
    return new ODCSGenerator(ontologyInfo, ontologyInfo.getConcepts(), adapters,
        odcsGeneratorProperties, schemaGeneratorProperties, diagramGeneratorProperties);
  }

  private List<AbstractAdapter<?>> selectAdapters(Map<String, AbstractAdapter<?>> adapterBeans,
      List<String> requestedAdapterNames) {
    List<AbstractAdapter<?>> available = new ArrayList<>(adapterBeans.values());
    if (requestedAdapterNames == null || requestedAdapterNames.isEmpty()) {
      available.sort(new be.vlaanderen.omgeving.oddtoolkit.adapter.AdapterDependencyComparator());
      return available;
    }

    Map<String, AbstractAdapter<?>> beanNameToAdapter = adapterBeans.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    List<AbstractAdapter<?>> selected = new ArrayList<>();
    for (String name : requestedAdapterNames) {
      if (beanNameToAdapter.containsKey(name)) {
        selected.add(beanNameToAdapter.get(name));
      } else {
        logger.warn("Requested adapter '{}' is not available or is disabled; ignoring.", name);
      }
    }
    selected.sort(new be.vlaanderen.omgeving.oddtoolkit.adapter.AdapterDependencyComparator());
    return selected;
  }
}

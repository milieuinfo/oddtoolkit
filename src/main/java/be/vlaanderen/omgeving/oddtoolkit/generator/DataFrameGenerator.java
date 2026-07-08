package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.DataFrameGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;

@Getter
public class DataFrameGenerator extends BaseGenerator {
  private ObjectMapper objectMapper = new ObjectMapper();
  private ObjectNode context = objectMapper.createObjectNode();
  private ObjectNode frame = objectMapper.createObjectNode();
  private final DataFrameGeneratorProperties dataFrameGeneratorProperties;
  private List<String> types = new ArrayList<>();
  private List<PropertyInfo> propertyAggregation = new ArrayList<>();

  private static final Comparator<String> STRING_ORDER = Comparator.nullsLast(String::compareTo);
  private static final Comparator<PropertyInfo> PROPERTY_ORDER = Comparator
      .comparing((PropertyInfo p) -> p != null ? p.getUri() : null, Comparator.nullsLast(String::compareTo))
      .thenComparing(p -> p != null ? p.getName() : null, Comparator.nullsLast(String::compareTo));

  public DataFrameGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      DataFrameGeneratorProperties dataFrameGeneratorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
    this.dataFrameGeneratorProperties = dataFrameGeneratorProperties;
  }

  @Override
  public void run() {
    super.run();
    prepare();
    writeFrame();
  }

  public void prepare() {
    getOntologyClasses().forEach(classInfo -> {
      this.types.add(classInfo.getUri());
      this.propertyAggregation.addAll(classInfo.getProperties());
    });

    getOntologyClassConcepts().forEach(classInfo -> this.types.add(classInfo.getUri()));

    this.types = this.types.stream()
        .filter(type -> type != null && !type.isBlank())
        .distinct()
        .sorted(STRING_ORDER)
        .toList();

    this.propertyAggregation = this.propertyAggregation.stream()
        .filter(property -> property != null && property.getUri() != null)
        .sorted(PROPERTY_ORDER)
        .toList();
  }

  public void writeFrame() {
    context.put("uri", "@id");
    ObjectNode typeContext = objectMapper.createObjectNode();
    typeContext.put("@type", "@id");
    typeContext.put("@id", "@type");
    typeContext.put("@container", "@set");
    context.put("type", typeContext);
    propertyAggregation.forEach(propertyInfo -> {
      ObjectNode propertyContext = objectMapper.createObjectNode();
      propertyContext.put("@id", propertyInfo.getUri());
      propertyContext.put("@type", "@id");
      context.put(propertyInfo.getName(), propertyContext);
    });
    frame.put("@context", context);
    ArrayNode properties = objectMapper.createArrayNode();
    types.forEach(properties::add);
    frame.put("@type", properties);
    frame.put("@embed", "@always");
    propertyAggregation.forEach(propertyInfo -> {
      ObjectNode propertyNode = objectMapper.createObjectNode();
      propertyNode.put("@embed", "@never");
      propertyNode.put("@omitDefault", true);
      frame.set(propertyInfo.getName(), propertyNode);
    });

    String outputFile = dataFrameGeneratorProperties.getOutputFile();
    if (outputFile != null) {
      saveToFile(outputFile, frame.toPrettyString());
    } else {
      System.out.println(frame.toPrettyString());
    }
  }
}

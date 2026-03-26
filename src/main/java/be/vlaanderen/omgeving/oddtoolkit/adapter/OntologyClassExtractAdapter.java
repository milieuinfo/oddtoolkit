package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AdapterDependency({
    OntologyReasonerAdapter.class,
})
@ConditionalOnConfigProperty(prefix = "adapters", name = "ontology-class-extract.enabled", havingValue = "true", matchIfMissing = true)
public class OntologyClassExtractAdapter extends AbstractAdapter<OntologyInfo> {

  public static final Logger logger = LoggerFactory.getLogger(OntologyClassExtractAdapter.class);

  public OntologyClassExtractAdapter() {
    super(OntologyInfo.class);
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    // Extract the owl classes from the loaded model
    ResIterator classIterator = info.getModel().listResourcesWithProperty(
        RDF.type,
        OWL2.Class
    );

    // Build a mutable list of ClassInfo instances
    List<ClassInfo> initialClasses = new ArrayList<>();
    classIterator.forEachRemaining(
        resource -> initialClasses.add(new ClassInfo(Scope.ONTOLOGY, resource)));

    // Populate OntologyInfo.classes (internal map) ensuring uniqueness by URI
    info.setClasses(initialClasses);

    logger.info("Extracted {} classes from the ontology", info.getClasses().size());

    // For each class, extract its superclasses
    List<ClassInfo> snapshot = new ArrayList<>(initialClasses);
    for (ClassInfo classInfo : snapshot) {
      this.extractSuperClasses(classInfo, info);
      // Add discovered superclasses to ontology info uniquely by URI
      for (ClassInfo superClass : classInfo.getSuperClasses()) {
        info.addClass(superClass);
      }
    }

    updateReferences(info);
    return info;
  }

  private void updateReferences(OntologyInfo info) {
    // Update the references to the class in superclasses
    for (ClassInfo classInfo : info.getClasses()) {
      // Update references in superclasses
      List<ClassInfo> superClasses = new ArrayList<>(classInfo.getSuperClasses());
      for (ClassInfo superClass : superClasses) {
        ClassInfo updatedSuperClass = info.getClassByUri(superClass.getUri());
        // Check if the superClass is the same instance as the updatedSuperClass, if not update the reference
        if (updatedSuperClass != null && updatedSuperClass != superClass) {
          classInfo.getSuperClasses().remove(superClass);
          classInfo.getSuperClasses().add(updatedSuperClass);
        }
      }
    }
  }

  private void extractSuperClasses(ClassInfo classInfo, OntologyInfo info) {
    logger.info("Extracting superclasses for class {}", classInfo.getResource().getLocalName());
    if (info.getInferredModel() != null) {
      // Get all the superclasses of the class from the inferred model
      info.getInferredModel()
          .listStatements(classInfo.getResource(), RDFS.subClassOf, (Resource) null)
          .forEachRemaining(statement -> {
            // Skip OWL restrictions as they are not actual superclasses but rather constraints on the class
            // Skip if the class is equal to the superclass to avoid circular references
            if (isClassStatement(statement) && !statement.getObject().asResource().getURI()
                .equals(classInfo.getUri())) {
              ClassInfo superClass = extractClassFromInferredModel(statement.getResource(),
                  classInfo, info);
              classInfo.getSuperClasses().add(superClass);
            }
          });
    } else {
      classInfo.getResource().listProperties(RDFS.subClassOf).forEachRemaining(statement -> {
        // Skip OWL restrictions as they are not actual superclasses but rather constraints on the class
        if (isClassStatement(statement)) {
          ClassInfo superClass = extractClassFromInferredModel(statement.getResource(),
              classInfo, info);
          classInfo.getSuperClasses().add(superClass);
        }
      });
    }
    logger.info("Extracted superclasses from the ontology: {}", classInfo.getSuperClasses().size());
  }

  private boolean isClassStatement(Statement statement) {
    return statement.getObject().isResource() && !statement.getObject().asResource()
        .hasProperty(RDF.type, OWL2.Restriction)
        && statement.getObject().asResource().getNameSpace() != null;
  }

  private ClassInfo extractClassFromInferredModel(Resource resource, ClassInfo classInfo,
      OntologyInfo info) {
    // If the resource URI is in the same namespace then the scope is ONTOLOGY else EXTERNAL
    Scope scope = resource.getNameSpace()
        .equals(classInfo.getResource().getNameSpace())
        ? Scope.ONTOLOGY
        : Scope.EXTERNAL;
    // Extract the owl classes from the inferred model
    if (info.getInferredModel() != null) {
      // Find the resource in the inferred model specifically with the same URI
      Resource inferredResource = info.getInferredModel().getResource(resource.getURI());
      if (inferredResource != null) {
        return new ClassInfo(scope, inferredResource);
      }
    }
    return new ClassInfo(scope, resource);
  }
}

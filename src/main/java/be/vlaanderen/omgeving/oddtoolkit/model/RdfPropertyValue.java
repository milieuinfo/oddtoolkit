package be.vlaanderen.omgeving.oddtoolkit.model;

import java.net.URI;
import java.util.Objects;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

/** A direct RDF property value, suitable for storing in an RDF metadata companion. */
public record RdfPropertyValue(String predicate, ObjectKind objectKind, String value,
                               String datatype, String language) {
  public enum ObjectKind { IRI, LITERAL }

  public RdfPropertyValue {
    requireIri(predicate, "predicate");
    Objects.requireNonNull(objectKind, "objectKind");
    Objects.requireNonNull(value, "value");
    if (objectKind == ObjectKind.IRI) {
      requireIri(value, "value");
      if (datatype != null || language != null) {
        throw new IllegalArgumentException("IRI values cannot have a datatype or language");
      }
    } else {
      if (datatype != null) {
        requireIri(datatype, "datatype");
      }
      if (language != null && language.isBlank()) {
        throw new IllegalArgumentException("Language must not be blank");
      }
      if (language != null && datatype != null && !RDF.langString.getURI().equals(datatype)) {
        throw new IllegalArgumentException("Language-tagged literals must use rdf:langString");
      }
      if (language == null && RDF.langString.getURI().equals(datatype)) {
        throw new IllegalArgumentException("rdf:langString requires a language");
      }
    }
  }

  public static RdfPropertyValue fromStatement(Statement statement) {
    Objects.requireNonNull(statement, "statement");
    RDFNode object = statement.getObject();
    if (object.isURIResource()) {
      return new RdfPropertyValue(statement.getPredicate().getURI(), ObjectKind.IRI,
          object.asResource().getURI(), null, null);
    }
    if (!object.isLiteral()) {
      throw new IllegalArgumentException("Blank-node values are not supported");
    }
    Literal literal = object.asLiteral();
    String language = literal.getLanguage();
    return new RdfPropertyValue(statement.getPredicate().getURI(), ObjectKind.LITERAL,
        literal.getLexicalForm(), literal.getDatatypeURI(),
        language == null || language.isEmpty() ? null : language);
  }

  public Statement toStatement(Resource subject) {
    Objects.requireNonNull(subject, "subject");
    Model model = Objects.requireNonNull(subject.getModel(), "subject model");
    RDFNode object;
    if (objectKind == ObjectKind.IRI) {
      object = model.createResource(value);
    } else if (language != null) {
      object = model.createLiteral(value, language);
    } else if (datatype != null) {
      RDFDatatype rdfDatatype = TypeMapper.getInstance().getSafeTypeByName(datatype);
      if (!rdfDatatype.isValid(value)) {
        throw new IllegalArgumentException("Invalid lexical value for datatype " + datatype);
      }
      object = model.createTypedLiteral(value, rdfDatatype);
    } else {
      object = model.createLiteral(value);
    }
    return model.createStatement(subject, model.createProperty(predicate), object);
  }

  private static void requireIri(String value, String field) {
    Objects.requireNonNull(value, field);
    try {
      if (!URI.create(value).isAbsolute()) {
        throw new IllegalArgumentException(field + " must be an absolute IRI");
      }
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(field + " must be an absolute IRI: " + value, ex);
    }
  }
}

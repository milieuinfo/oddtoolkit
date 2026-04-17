package be.vlaanderen.omgeving.oddtoolkit.model;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
public abstract class AbstractInfo {
  private String uri;
  private String name;
  private String label;
  private String comment;
  private Resource resource;
  private final Scope scope;

  public AbstractInfo(Scope scope, @Nullable Resource resource) {
    this.scope = scope;
    if (resource != null) {
      initializeFromResource(resource);
    }
  }

  protected void initializeFromResource(Resource resource) {
    this.resource = resource;
    this.name = resource.getLocalName();
    if (this.uri == null) {
      this.uri = resource.getURI();
    }
    Statement labelStmt = resource.getProperty(RDFS.label);
    if (labelStmt != null && labelStmt.getObject().isLiteral()) {
      this.label = labelStmt.getString();
    } else {
      this.label = null;
    }
    Statement commentStmt = resource.getProperty(RDFS.comment);
    if (commentStmt != null && commentStmt.getObject().isLiteral()) {
      this.comment = commentStmt.getString();
    } else {
      this.comment = null;
    }
  }

  public String toString() {
    return getName();
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractInfo that = (AbstractInfo) o;
    return Objects.equals(uri, that.uri);
  }

  public int hashCode() {
    return Objects.hash(uri);
  }
}

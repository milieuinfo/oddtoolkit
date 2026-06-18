package be.vlaanderen.omgeving.oddtoolkit.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigPrefix("generators.schema-generator")
public class SchemaGeneratorProperties {
  private MergeJoinTables mergeJoinTables = new MergeJoinTables();
  private IdentityTables identityTables = new IdentityTables();
  private JoinTableColumns joinTableColumns = new JoinTableColumns();
  private String joinTableNamePattern = "rel_{source_table}_{target_table}";

  /**
   * Backward-compatible flat accessor for {@code generators.schema-generator.source-column-name-pattern}.
   */
  public String getSourceColumnNamePattern() {
    return joinTableColumns.getSourceColumnNamePattern();
  }

  public void setSourceColumnNamePattern(String sourceColumnNamePattern) {
    joinTableColumns.setSourceColumnNamePattern(sourceColumnNamePattern);
  }

  /**
   * Backward-compatible flat accessor for {@code generators.schema-generator.target-column-name-pattern}.
   */
  public String getTargetColumnNamePattern() {
    return joinTableColumns.getTargetColumnNamePattern();
  }

  public void setTargetColumnNamePattern(String targetColumnNamePattern) {
    joinTableColumns.setTargetColumnNamePattern(targetColumnNamePattern);
  }

  @Getter
  @Setter
  public static class MergeJoinTables {

    private boolean enabled = true;
    private String attributeName = "relation_type";
    private List<ExcludedPair> excludedPairs = new ArrayList<>();
  }

  @Getter
  @Setter
  public static class ExcludedPair {
    private String sourceUri;
    private String targetUri;

    public boolean matches(String leftUri, String rightUri) {
      if (sourceUri == null || targetUri == null || leftUri == null || rightUri == null) {
        return false;
      }
      return (sourceUri.equals(leftUri) && targetUri.equals(rightUri))
          || (sourceUri.equals(rightUri) && targetUri.equals(leftUri));
    }
  }

  @Getter
  @Setter
  public static class IdentityTables {
    private boolean enabled = true;
    private String tableNameSuffix = "identity";
  }

  /**
   * Configures the naming of the identifier columns inside a many-to-many join table.
   *
   * <p>Both patterns support the following placeholders:
   * <ul>
   *   <li>{@code {source_table}} – the name of the source (from) table</li>
   *   <li>{@code {target_table}} – the name of the target (to) table</li>
   *   <li>{@code {column}} – the original identifier column name</li>
   * </ul>
   *
   * <p>Examples:
   * <pre>
   *   sourceColumnNamePattern = "source_{column}"          →  source_uuid
   *   sourceColumnNamePattern = "{source_table}_{column}"  →  exploitatie_uuid
   *   sourceColumnNamePattern = "{source_table}_id"        →  exploitatie_id
   * </pre>
   */
  @Getter
  @Setter
  public static class JoinTableColumns {
    private String sourceColumnNamePattern = "source_{column}";
    private String targetColumnNamePattern = "target_{column}";
  }
}

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
  private String joinTableNamePattern = "rel_{source_table}_{target_table}";

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
}

package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Typed properties for the ontology reasoner adapter.
 */
@Getter
@Setter
@ConfigPrefix("adapters.ontology-reasoner")
public class OntologyReasonerProperties {
  private boolean enabled = true;

  // rules file for GenericRuleReasoner (optional)
  private String rulesFile = "";

  // reasoner type: 'owl' or 'rdfs'
  private String reasonerType = "owl";

  // whether to materialize (call InfModel.prepare())
  private boolean reasonerMaterialize = false;

  // timeout for reasoning (ms), 0 = disabled
  private long reasonerTimeoutMs = 0L;

  // inferred-model caching
  private boolean inferredCacheEnabled = true;
  private long inferredCacheTtlMs = 3600000L;
  private String inferredCacheDir = "target/cache/inferred";
  private String inferredCacheFormat = "TURTLE";

  // optional output of inferred model
  private boolean inferredOutputEnabled = false;
  private String inferredOutputPath = "";
}

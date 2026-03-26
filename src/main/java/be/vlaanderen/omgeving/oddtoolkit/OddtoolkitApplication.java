package be.vlaanderen.omgeving.oddtoolkit;

import be.vlaanderen.omgeving.oddtoolkit.cli.GeneratorCliRunner;
import be.vlaanderen.omgeving.oddtoolkit.config.GeneratorRegistry;
import be.vlaanderen.omgeving.oddtoolkit.config.OddtoolkitBootstrap;

public class OddtoolkitApplication {

  public static void main(String[] args) throws Exception {
    GeneratorRegistry registry = OddtoolkitBootstrap.bootstrap(args);
    new GeneratorCliRunner(registry).run(args);
  }
}

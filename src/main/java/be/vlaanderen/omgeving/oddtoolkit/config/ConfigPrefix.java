package be.vlaanderen.omgeving.oddtoolkit.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a POJO as bindable from a configuration subtree (dot-path, kebab-case keys).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigPrefix {
  String value();
}


package be.vlaanderen.omgeving.oddtoolkit.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lightweight replacement for Spring's ConditionalOnProperty.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConditionalOnConfigProperty {
  String prefix();

  String name();

  String havingValue() default "true";

  boolean matchIfMissing() default true;
}


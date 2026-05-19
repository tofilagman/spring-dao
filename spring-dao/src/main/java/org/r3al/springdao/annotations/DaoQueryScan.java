package org.r3al.springdao.annotations;

import org.r3al.springdao.DaoQueryScanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opt-in override for the default package scan. By default spring-dao scans the
 * packages registered by {@code @SpringBootApplication} / {@code @AutoConfigurationPackage}.
 * Add this annotation to a {@code @Configuration} class when your DaoQuery interfaces
 * live outside the application's auto-configured packages.
 *
 * <pre>{@code
 * @Configuration
 * @DaoQueryScan(basePackages = "com.example.repos")
 * public class DaoQueryConfig { }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DaoQueryScanRegistrar.class)
public @interface DaoQueryScan {

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};
}

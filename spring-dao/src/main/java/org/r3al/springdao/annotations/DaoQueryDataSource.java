package org.r3al.springdao.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Routes a DaoQuery interface to a specific datasource by Spring bean qualifier.
 * When present, the library resolves EntityManager / NamedParameterJdbcTemplate via
 * {@code BeanFactoryAnnotationUtils.qualifiedBeanOfType} using the given qualifier
 * instead of an unqualified lookup. When absent, falls back to the primary bean.
 *
 * <pre>{@code
 * @DaoQueryDataSource("reporting")
 * public interface ReportingDaoQuery extends DaoQuery { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DaoQueryDataSource {

    String value();
}

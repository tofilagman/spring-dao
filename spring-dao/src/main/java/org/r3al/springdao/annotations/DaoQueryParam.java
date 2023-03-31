package org.r3al.springdao.annotations;

import org.r3al.springdao.DaoQueryActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DaoQueryParam {
    String value();
    boolean addChildren() default false;
    DaoQueryActionType action() default DaoQueryActionType.DEFAULT;
}

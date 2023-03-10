package org.r3al.springdao.annotations

import org.r3al.springdao.DaoQueryOperator
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    RetentionPolicy.RUNTIME
)
annotation class DaoQueryParam(
    val value: String,
    val operator: DaoQueryOperator = DaoQueryOperator.DEFAULT,
    val addChildren: Boolean = false
)
package org.r3al.springdao

import java.util.function.Function

enum class DaoQueryOperator(val transformParam: Function<Any?, Any?>) {
    DEFAULT(Function { value: Any? -> value })
}
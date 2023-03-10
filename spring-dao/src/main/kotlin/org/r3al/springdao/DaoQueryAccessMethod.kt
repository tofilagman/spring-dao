package org.r3al.springdao

import org.r3al.springdao.annotations.DaoQueryParam
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

class DaoQueryAccessMethod(val method: Method) {
    val name: String
    var type: Class<*>? = null
    var param: DaoQueryParam? = null

    init {
        name = method.name.substring(if (method.name.startsWith("get")) 3 else 2)
        type = if (method.annotatedReturnType.type is ParameterizedType) {
            (method.annotatedReturnType.type as ParameterizedType).rawType as Class<*>
        } else {
            method.annotatedReturnType.type as Class<*>
        }
        if (method.isAnnotationPresent(DaoQueryParam::class.java)) {
            param = method.getAnnotation(DaoQueryParam::class.java)
        }
    }
}
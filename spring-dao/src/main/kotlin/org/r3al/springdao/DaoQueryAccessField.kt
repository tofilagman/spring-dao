package org.r3al.springdao

import org.apache.commons.text.WordUtils
import org.r3al.springdao.annotations.DaoQueryParam
import java.lang.reflect.Field

class DaoQueryAccessField(field: Field) {
    val name: String
    val type: Class<*>
    var param: DaoQueryParam? = null

    init {
        name = WordUtils.capitalize(field.name)
        type = field.type
        if (field.isAnnotationPresent(DaoQueryParam::class.java)) {
            param = field.getAnnotation(DaoQueryParam::class.java)
        }
    }
}
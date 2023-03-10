package org.r3al.springdao

import org.apache.commons.text.WordUtils
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.lang.reflect.Method

class DaoQueryParameter(val name: String, val value: Any?) : Serializable, Cloneable {
    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        return super.clone()
    }

    override fun toString(): String {
        return """
            DaoQueryParameter {
                name='$name',
                value='$value'
            }
        """.trimIndent()
    }

    companion object {
        // todo adicionar log
        private val LOGGER = LoggerFactory.getLogger(DaoQueryParameter::class.java)
        fun ofDeclaredMethods(parentName: String, classe: Class<*>?, `object`: Any?): List<DaoQueryParameter?> {
            val parameterList = ArrayList<DaoQueryParameter?>()
            val fieldInfoMap = DaoQueryCache.getFieldInfo(classe)
            val accessMethods = DaoQueryCache.getAccessMethods(classe)
            for (accessMethod in accessMethods) {
                val value = getValue(`object`, accessMethod.method)
                val fieldInfo = fieldInfoMap[accessMethod.name]
                if (fieldInfo != null) {
                    val queryParam = fieldInfo.param ?: accessMethod.param
                    if (queryParam != null) {
                        if (queryParam.addChildren) {
                            val parentNameChildren = parentName + WordUtils.capitalize(queryParam.value)
                            parameterList.addAll(ofDeclaredMethods(parentNameChildren, fieldInfo.type, value))
                        } else {
                            val paramName = parentName + WordUtils.capitalize(queryParam.value)
                            if (value is Map<*, *>) {
                                parameterList.addAll(ofMap(value, paramName))
                            } else {
                                val paramValue = queryParam.operator.transformParam.apply(value)
                                parameterList.add(DaoQueryParameter(paramName, paramValue))
                            }
                        }
                    } else {
                        if (value is Map<*, *>) {
                            parameterList.addAll(ofMap(value, parentName + accessMethod.name))
                        } else {
                            val paramValue = DaoQueryOperator.DEFAULT.transformParam.apply(value)
                            parameterList.add(DaoQueryParameter(parentName + accessMethod.name, paramValue))
                        }
                    }
                }
            }
            return parameterList
        }

        private fun getValue(`object`: Any?, method: Method?): Any? {
            return try {
                method!!.invoke(`object`)
            } catch (ignore: Exception) {
                null
            }
        }

        fun ofMap(map: Map<*, *>, name: String): List<DaoQueryParameter?> {
            val parameterList = ArrayList<DaoQueryParameter?>()
            parameterList.add(DaoQueryParameter(name, map.keys))
            map.forEach {
                parameterList.add(DaoQueryParameter(it.key.toString(), it.value))
            }
            return parameterList
        }
    }
}
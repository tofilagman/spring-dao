package org.r3al.springdao

import org.aopalliance.intercept.MethodInvocation
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.HashMap

internal object DaoQueryCache {
    private val LOGGER = LoggerFactory.getLogger(DaoQueryCache::class.java)
    private val CACHE_DAO_QUERY_INFO: MutableMap<DaoQueryInfoKey, DaoQueryInfo?> = HashMap()
    private val CACHE_FIELD_INFO: MutableMap<String, MutableMap<String?, DaoQueryFieldInfo?>> = HashMap()
    private val CACHE_ACCESS_METHODS: MutableMap<String, MutableList<DaoQueryAccessMethod>> = HashMap()

    operator fun get(classe: Class<out DaoQuery>, invocation: MethodInvocation): DaoQueryInfo? {
        val daoQueryInfoKey = DaoQueryInfoKey(
            classe.name,
            invocation.method.name
        )
        LOGGER.debug("information cache key {}", daoQueryInfoKey.toString())
        var info = CACHE_DAO_QUERY_INFO[daoQueryInfoKey]
        if (info == null) {
            info = DaoQueryInfo.of(classe, invocation)
            LOGGER.debug("caching method {} information from interface {}", invocation.method.name, classe.name)
            CACHE_DAO_QUERY_INFO[daoQueryInfoKey] = info
        } else {
            info = try {
                LOGGER.debug(
                    "getting from the cache the information of method {} of class {}",
                    invocation.method.name,
                    classe.name
                )
                info.clone() as DaoQueryInfo
            } catch (e: CloneNotSupportedException) {
                LOGGER.debug(
                    "error in cloning the information that was cached in method {} of class {}",
                    invocation.method.name,
                    classe.name
                )
                throw RuntimeException(e)
            }
        }
        DaoQueryInfo.setParameters(info, invocation)
        return info
    }

    fun getAccessMethods(classe: Class<*>?): List<DaoQueryAccessMethod> {
        val className = classe!!.name
        var methods = CACHE_ACCESS_METHODS[className]
        if (methods == null) {
            methods = ArrayList()
            for (method in classe.declaredMethods) {
                if (method.name.startsWith("get") || method.name.startsWith("is")) {
                    methods.add(DaoQueryAccessMethod(method))
                }
            }
            CACHE_ACCESS_METHODS[className] = methods
        }
        return methods
    }

    fun getFieldInfo(classe: Class<*>?): Map<String?, DaoQueryFieldInfo?> {
        val className = classe!!.name
        var fieldInfoMap = CACHE_FIELD_INFO[className]
        if (fieldInfoMap == null) {
            fieldInfoMap = HashMap()
            val accessFields = getAccessFields(classe)
            for (accessField in accessFields) {
                fieldInfoMap[accessField.name] = DaoQueryFieldInfo(accessField.param, accessField.type)
            }
            val accessMethods = getAccessMethods(classe)
            for (accessMethod in accessMethods) {
                if (fieldInfoMap[accessMethod.name] == null) {
                    fieldInfoMap[accessMethod.name] = DaoQueryFieldInfo(accessMethod.param, accessMethod.type)
                }
            }
            CACHE_FIELD_INFO[className] = fieldInfoMap
        }
        return fieldInfoMap
    }

    private fun getAccessFields(classe: Class<*>?): List<DaoQueryAccessField> {
        val fields: MutableList<DaoQueryAccessField> = ArrayList()
        for (field in classe!!.declaredFields) {
            fields.add(DaoQueryAccessField(field))
        }
        return fields
    }

    private class DaoQueryInfoKey(var className: String, var methodName: String) {
        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val nativeQueryInfoKey = o as DaoQueryInfoKey
            return className == nativeQueryInfoKey.className && methodName == nativeQueryInfoKey.methodName
        }

        override fun hashCode(): Int {
            return Objects.hash(className, methodName)
        }

        override fun toString(): String {
            return """
                DaoQueryInfoKey{
                    className='$className',
                    methodName='$methodName'
                }
            """.trimIndent()
        }
    }
}
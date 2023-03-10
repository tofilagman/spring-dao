package org.r3al.springdao

import jdk.jshell.spi.ExecutionControl.NotImplementedException
import org.aopalliance.intercept.MethodInvocation
import org.r3al.springdao.annotations.DaoQueryParam
import org.r3al.springdao.annotations.DaoQuerySql
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.util.ClassTypeInformation
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

class DaoQueryInfo private constructor() : Serializable, Cloneable {

    var parameterList: ArrayList<DaoQueryParameter?>? = null
    var aliasToBean: Class<*>? = null
    private var returnTypeIsIterable = false
    var returnType: Class<*>? = null
    private var sql: String? = null
    private var sqlInline: String? = null
    private var useSqlInline = false

//    var isUseJdbcTemplate = false 
//        private set

    fun getSql(): String? {
//        if (sql != null) {
//            return sql
//        }
//        sql = sqlProcessed
//        for (aClass in processorSqlList) {
//            try {
//                val processor = aClass.newInstance()
//                processor.execute(sql, replaceSql)
//            } catch (e: Exception) {
//                throw RuntimeException(e.message)
//            }
//        }
//        for ((key, value) in replaceSql) {
//            sql = sql!!.replace("\\$\\{$key}".toRegex(), value)
//        }
//        if (sort != null) {
//            val orderBuilder = StringBuilder()
//            for (order in sort!!) {
//                if (orderBuilder.length == 0) {
//                    orderBuilder.append(" ORDER BY ")
//                } else {
//                    orderBuilder.append(", ")
//                }
//                orderBuilder.append(order.property)
//                    .append(" ")
//                    .append(order.direction.name)
//                val nulls = order.nullHandling
//                if (nulls != Sort.NullHandling.NATIVE) {
//                    orderBuilder.append(" ")
//                        .append(nulls.name.replace('_', ' '))
//                }
//            }
//            sql += orderBuilder.toString()
//        }
//        if (useTenant) {
//            val tenantJdbcTemplate: NativeQueryTenantNamedParameterJdbcTemplateInterceptor =
//                ApplicationContextProvider.applicationContext.getBean(
//                    NativeQueryTenantNamedParameterJdbcTemplateInterceptor::class.java
//                )
//            sql = tenantJdbcTemplate.tenant?.let { sql!!.replace(":SCHEMA", it) }
//        }
//        LOGGER.debug("SQL to be executed: {}", sql)
//        return sql
        throw NotImplementedException("TODO")
    }

    val isJavaObject: Boolean
        get() {
            val isJavaObject = getPackageName(aliasToBean).startsWith("java")
            LOGGER.debug("is java Object {}", isJavaObject)
            return isJavaObject
        }

    val isSingleResult: Boolean
        get() {
            val isSingleResult = !returnTypeIsIterable
            LOGGER.debug("is single result {}", isSingleResult)
            return isSingleResult
        }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }

    fun returnTypeIsOptional(): Boolean {
        val typeIsOptional = returnType!!.simpleName == Optional::class.java.simpleName
        LOGGER.debug("Return type is optional {}", typeIsOptional)
        return typeIsOptional
    }

    private fun getPackageName(c: Class<*>?): String {
        var c = c
        while (c!!.isArray) {
            c = c.componentType
        }
        val pn: String = if (c.isPrimitive) {
            "java.lang"
        } else {
            val cn = c.name
            val dot = cn.lastIndexOf('.')
            if (dot != -1) cn.substring(0, dot).intern() else ""
        }
        return pn
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DaoQueryInfo::class.java)
        fun of(classe: Class<out DaoQuery>, invocation: MethodInvocation): DaoQueryInfo {
            val info = DaoQueryInfo()
            val method = invocation.method
            LOGGER.debug("invoked method {}", method.name)
            info.useSqlInline = method.isAnnotationPresent(DaoQuerySql::class.java)
            if (info.useSqlInline) {
                LOGGER.debug("sql obtained using the DaoQuerySql annotation")
                info.sqlInline = method.getAnnotation(DaoQuerySql::class.java).value
            } else {
                LOGGER.debug("sql obtained using xml")
                //setFile(classe, invocation, info)
            }
//            if (method.isAnnotationPresent(NativeQueryUseHibernateTypes::class.java)) {
//                info.isUseHibernateTypes =
//                    method.getAnnotation(NativeQueryUseHibernateTypes::class.java).useHibernateTypes()
//            } else {
//                info.isUseHibernateTypes =
//                    java.lang.Boolean.parseBoolean(PropertyUtil.getValue("native-query.use-hibernate-types", "true"))
//            }
            // LOGGER.debug("use hibernate types {}", info.isUseHibernateTypes)
//            info.isUseJdbcTemplate = method.isAnnotationPresent(NativeQueryUseJdbcTemplate::class.java)
//            if (info.isUseJdbcTemplate) {
//                LOGGER.debug("use JdbcTemplate")
//                val jdbcTemplate = method.getAnnotation(
//                    NativeQueryUseJdbcTemplate::class.java
//                )
//                info.useTenant = jdbcTemplate.useTenant()
//                LOGGER.debug("use JdbcTemplate with tenant {}", info.useTenant)
//            }
//            if (method.isAnnotationPresent(NativeQueryReplaceSql::class.java)) {
//                if (method.getAnnotation<NativeQueryReplaceSql>(NativeQueryReplaceSql::class.java).values().size > 0) {
//                    LOGGER.debug("makes use of sql change")
//                    for (value in method.getAnnotation<NativeQueryReplaceSql>(
//                        NativeQueryReplaceSql::class.java
//                    ).values()) {
//                        LOGGER.debug("replace key {} and value {}", value.key(), value.value())
//                        info.replaceSql[value.key()] = value.value()
//                    }
//                    info.processorSqlList.addAll(
//                        Arrays.asList(
//                            *method.getAnnotation(
//                                NativeQueryReplaceSql::class.java
//                            ).processorParams()
//                        )
//                    )
//                }
//            }
            info.returnType = method.returnType
            LOGGER.debug("return type {}", info.returnType?.name)
            info.returnTypeIsIterable = Iterable::class.java.isAssignableFrom(info.returnType)
            LOGGER.debug("return type is iterable {}", info.returnTypeIsIterable)
            if (info.returnTypeIsIterable || info.returnTypeIsOptional()) {
                val componentType = ClassTypeInformation.fromReturnTypeOf<Any>(method).componentType
                info.aliasToBean = Objects.requireNonNull(componentType).type
            } else {
                info.aliasToBean = info.returnType
            }
            LOGGER.debug("return object is {}", info.aliasToBean!!.name)
            return info
        }

        fun setParameters(info: DaoQueryInfo?, invocation: MethodInvocation) {

            if (info == null)
                return

            info.sql = null
            //info.sort = null
            info.parameterList = ArrayList()
            // info.pageable = null
            for (i in invocation.arguments.indices) {
                val argument = invocation.arguments[i]
                val parameter = invocation.method.parameters[i]
//                if (parameter.type.isAssignableFrom(Pageable::class.java)) {
//                    info.pageable = argument as Pageable
//                    if (info.sort == null) {
//                        info.sort = info.pageable!!.sort
//                    }
//                } else if (parameter.type.isAssignableFrom(Sort::class.java)) {
//                    info.sort = argument as Sort
//                } else {
                if (parameter.isAnnotationPresent(DaoQueryParam::class.java)) {
                    val param = parameter.getAnnotation(
                        DaoQueryParam::class.java
                    )
                    if (param.addChildren) {
                        info.parameterList?.addAll(
                            DaoQueryParameter.ofDeclaredMethods(
                                param.value,
                                parameter.type,
                                argument
                            )
                        )
                    } else {
                        if (argument is Map<*, *>) {
                            info.parameterList?.addAll(
                                DaoQueryParameter.ofMap(
                                    argument, param.value
                                )
                            )
                        } else {
                            info.parameterList?.add(
                                DaoQueryParameter(
                                    param.value,
                                    param.operator.transformParam.apply(argument)
                                )
                            )
                        }
                    }
                } else {
                    if (argument is Map<*, *>) {
                        info.parameterList?.addAll(DaoQueryParameter.ofMap(argument, parameter.name))
                    } else {
                        info.parameterList?.add(DaoQueryParameter(parameter.name, argument))
                    }
                }
                //}
            }
            info.parameterList?.forEach { parameter ->
                LOGGER.debug("Parameter {} containing the value {} added", parameter?.name, parameter?.value)
            }
        }
    }
}
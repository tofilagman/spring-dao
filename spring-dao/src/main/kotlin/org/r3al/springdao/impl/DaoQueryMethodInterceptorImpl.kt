package org.r3al.springdao.impl

import jakarta.persistence.NoResultException
import org.hibernate.query.NativeQuery
import org.r3al.springdao.ApplicationContextProvider
import org.r3al.springdao.DaoQueryInfo
import org.r3al.springdao.DaoQueryMethodInterceptor
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*
import java.util.function.Supplier
import kotlin.collections.HashMap

class DaoQueryMethodInterceptorImpl : DaoQueryMethodInterceptor {
    override fun executeQuery(info: DaoQueryInfo?): Any? {
//        return if (!info!!.isUseJdbcTemplate) {
//            executeWithEntityManager(info)
//        } else executeWithJdbcTemplate(info)
        return executeWithJdbcTemplate(info)
    }

    private fun executeWithJdbcTemplate(info: DaoQueryInfo?): Any? {
        LOGGER.debug("SQL will be executed with JdbcTemplate")
        LOGGER.debug("getting the instance of the NamedParameterJdbcTemplate bean")
        val jdbcTemplate: NamedParameterJdbcTemplate =
            ApplicationContextProvider.applicationContext.getBean(NamedParameterJdbcTemplate::class.java)
        val parametroList: MutableMap<String?, Any?> = HashMap()
//        for (parameter in info?.parameterList) {
//            LOGGER.debug("checking if parameter {} exists in sql", parameter.name)
//            if (parameter.value != null && info.sql!!.contains(":" + parameter.name)) {
//                LOGGER.debug("parameter {} exists in SQL", parameter.name)
//                LOGGER.debug(
//                    "parameter {} containing the value {} added to SQL",
//                    parameter.name,
//                    parameter.value.toString()
//                )
//                parametroList[parameter.name] = parameter.value
//            }
//        }
        info?.parameterList?.forEach { parameter ->

            if (parameter == null)
                return@forEach

            LOGGER.debug("checking if parameter {} exists in sql", parameter.name)
            if (parameter.value != null && info.getSql()?.contains(":" + parameter.name) == true) {
                LOGGER.debug("parameter {} exists in SQL", parameter.name)
                LOGGER.debug(
                    "parameter {} containing the value {} added to SQL",
                    parameter.name,
                    parameter.value.toString()
                )
                parametroList[parameter.name] = parameter.value
            }
        }

        LOGGER.debug("instantiating a BeanPropertyRowMapper of type {}", info?.aliasToBean?.name)
        val beanPropertyRowMapper: BeanPropertyRowMapper<*> = BeanPropertyRowMapper(info?.aliasToBean)
        if (info?.returnType?.simpleName == Void.TYPE.name) {
            LOGGER.debug("running update")
            info?.getSql()?.let { jdbcTemplate.update(it, parametroList) }
            return null
        }
        if (info?.isSingleResult == true) {
            if (info.isJavaObject) {
                LOGGER.debug(
                    "executing the query and returning an object of type {}", info.aliasToBean!!
                        .name
                )
                return info.getSql()?.let { jdbcTemplate.queryForObject(it, parametroList, info.aliasToBean) }
            }
            if (info.returnTypeIsOptional()) {
                LOGGER.debug(
                    "executing the query and returning an optional {}", info.aliasToBean!!
                        .name
                )
                return getOptionalReturn {
                    info.getSql()?.let {
                        jdbcTemplate.queryForObject(
                            it,
                            parametroList,
                            beanPropertyRowMapper
                        )
                    }
                }
            }
            LOGGER.debug("executing the query and returning an object of type {}", info.aliasToBean?.name)
            return info.getSql()?.let { jdbcTemplate.queryForObject(it, parametroList, beanPropertyRowMapper) }
        }
        LOGGER.debug("executing the query and returning a list of objects of type {}", info?.aliasToBean?.name)
        return if (info?.isJavaObject == true) {
            info.getSql()?.let { jdbcTemplate.queryForList(it, parametroList, info.aliasToBean) }
        } else {
            info?.getSql()?.let { jdbcTemplate.query(it, parametroList, beanPropertyRowMapper) }
        }
    }

//    private fun executeWithEntityManager(info: NativeQueryInfo?): Any? {
//        LOGGER.debug("SQL will be executed with EntityManager")
//        LOGGER.debug("getting the instance of the EntityManager bean")
//        val entityManager: EntityManager =
//            ApplicationContextProvider.Companion.getApplicationContext().getBean<EntityManager>(
//                EntityManager::class.java
//            )
//        val session = entityManager.unwrap(Session::class.java)
//        val query: NativeQuery<*>
//        if (info!!.isEntity()) {
//            LOGGER.debug(
//                "creating a native query with the entityManager and defining the return class {}", info.aliasToBean!!
//                    .name
//            )
//            query = session.createNativeQuery(info.sql, info.aliasToBean)
//        } else {
//            LOGGER.debug("creating a native query with the entityManager")
//            query = session.createNativeQuery(info.sql)
//        }
//        addParameterJpa(query, info)
//        if (info.hasPagination()) {
//            LOGGER.debug("setting pagination, first {}, max {}", info.firstResult, info.maxResult)
//            query.firstResult = info.firstResult
//            query.maxResults = info.maxResult
//        }
//        if (!info.isJavaObject && !info.isEntity()) {
//            if (info.isUseHibernateTypes) {
//                HibernateTypesMapper.map(query, info.aliasToBean)
//            }
//            LOGGER.debug(
//                "invoking Hibernate ResultTransformer to convert the SQL query to an object of type {}",
//                info.aliasToBean!!
//                    .name
//            )
//            query.setResultTransformer(Transformers.aliasToBean(info.aliasToBean))
//        }
//        if (info.returnType!!.simpleName == Void.TYPE.name) {
//            LOGGER.debug("running update")
//            query.executeUpdate()
//            return null
//        }
//        if (info.returnTypeIsOptional()) {
//            LOGGER.debug(
//                "executes the query returning an optional {}", info.aliasToBean!!
//                    .name
//            )
//            return getOptionalReturn(Supplier { query.singleResult })
//        }
//        if (info.isSingleResult) {
//            LOGGER.debug(
//                "executes the query by returning an {} object", info.aliasToBean!!
//                    .name
//            )
//            return query.singleResult
//        }
//        val resultList = query.list()
//        if (info.isPagination) {
//            LOGGER.debug("creating an object containing the pagination of the data returned in the query")
//            return PageImpl(resultList, info.pageable, getTotalRecords(info, session))
//        }
//        return resultList
//    }

    private fun getOptionalReturn(result: Supplier<Any?>): Any {
        return try {
            Optional.ofNullable(result.get())
        } catch (e: NoResultException) {
            Optional.empty()
        } catch (e: EmptyResultDataAccessException) {
            Optional.empty()
        }
    }

//    private fun getTotalRecords(info: DaoQueryInfo?, session: Session): Long {
//        LOGGER.debug("executing the query to obtain the number of records found to be used in the pagination")
//        val query = session.createNativeQuery(info.getSqlTotalRecord())
//        query.unwrap<NativeQuery<*>>(NativeQuery::class.java).addScalar("totalRecords", StandardBasicTypes.LONG)
//        addParameterJpa(query, info)
//        return query.singleResult as Long
//    }

    private fun addParameterJpa(query: NativeQuery<*>, info: DaoQueryInfo?) {
        info!!.parameterList!!.forEach { parameter ->

            if (parameter == null)
                return@forEach

            LOGGER.debug("checking if parameter {} exists in sql", parameter.name)
            if (parameter.value != null && info.getSql()?.contains(":" + parameter.name) == true) {
                LOGGER.debug("parameter {} exists in SQL", parameter.name)
                LOGGER.debug(
                    "parameter {} containing the value {} added to SQL",
                    parameter.name,
                    parameter.value.toString()
                )
                query.setParameter(parameter.name, parameter.value)
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DaoQueryMethodInterceptorImpl::class.java)
    }
}
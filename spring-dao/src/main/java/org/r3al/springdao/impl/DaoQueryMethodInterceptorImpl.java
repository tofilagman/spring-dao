package org.r3al.springdao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;
import org.r3al.springdao.*;
import org.r3al.springdao.mapper.HibernateTypesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DaoQueryMethodInterceptorImpl implements DaoQueryMethodInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryMethodInterceptorImpl.class);

    @Override
    public Object executeQuery(DaoQueryInfo info) throws IOException {
        if (!info.isUseJdbcTemplate()) {
            return executeWithEntityManager(info);
        }
        return executeWithJdbcTemplate(info);
    }

    private Object executeWithJdbcTemplate(DaoQueryInfo info) throws IOException {
        NamedParameterJdbcTemplate jdbcTemplate = ApplicationContextProvider.getApplicationContext().getBean(NamedParameterJdbcTemplate.class);

        Map<String, Object> parametroList = new HashMap<>();
        for (DaoQueryParameter parameter : info.getParameterList()) {
            if (parameter.getValue() != null && info.hasSqlParameter(parameter.getName(), DaoQueryTemplateDataType.QUERY)) {
                parametroList.put(parameter.getName(), parameter.getValue());
            }
        }
        if (!info.isUseSqlInline()) {
            LOGGER.debug("loading template {}: {}", info.getSqlKey(), info.getSqlPattern());
        }
        LOGGER.debug("executing: {}", info.getSql());
        LOGGER.debug("parsed: {}", new ObjectMapper().writeValueAsString(parametroList));

        BeanPropertyRowMapper<?> beanPropertyRowMapper = new BeanPropertyRowMapper<>(info.getAliasToBean());
        if (info.getReturnType().getSimpleName().equals(Void.TYPE.getName())) {
            jdbcTemplate.update(info.getSql(), parametroList);
            return null;
        }

        try {
            if (info.returnTypeIsListResult()) {
                DaoQueryListResult result = new DaoQueryListResult<>();

                if (info.isJavaObject()) {
                    result.setData(jdbcTemplate.queryForList(info.getSql(), parametroList, info.getAliasToBean()));
                }
                result.setData(jdbcTemplate.query(info.getSql(), parametroList, beanPropertyRowMapper));

                result.setCount(jdbcTemplate.queryForObject(info.getSqlCount(), parametroList, Integer.class));
                return result;
            }

            if (info.isSingleResult()) {
                if (info.isJavaObject()) {
                    if (info.getSqlReturn() != null) {
                        jdbcTemplate.update(info.getSql(), parametroList);
                        return jdbcTemplate.queryForObject(info.getSqlReturn(), parametroList, info.getAliasToBean());
                    }

                    return jdbcTemplate.queryForObject(info.getSql(), parametroList, info.getAliasToBean());
                }

                if (info.returnTypeIsOptional()) {
                    return getOptionalReturn(() -> {
                        try {
                            return jdbcTemplate.queryForObject(info.getSql(), parametroList, beanPropertyRowMapper);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    });
                }
                return jdbcTemplate.queryForObject(info.getSql(), parametroList, beanPropertyRowMapper);
            }

            if (info.isJavaObject()) {
                return jdbcTemplate.queryForList(info.getSql(), parametroList, info.getAliasToBean());
            }
            return jdbcTemplate.query(info.getSql(), parametroList, beanPropertyRowMapper);
        } catch (EmptyResultDataAccessException e) {
            LOGGER.debug("executing the query and returning an empty result of type {}", info.getAliasToBean().getName());
            return null;
        }
    }

    private Object executeWithEntityManager(DaoQueryInfo info) throws IOException {

        EntityManager entityManager = ApplicationContextProvider.getApplicationContext().getBean(EntityManager.class);
        Session session = entityManager.unwrap(Session.class);
        NativeQuery<?> query;

        try {

            query = session.createNativeQuery(info.getSql(), info.getAliasToBean());

            if (!info.isUseSqlInline()) {
                LOGGER.debug("loading template {}: {}", info.getSqlKey(), info.getSqlPattern());
            }

            LOGGER.debug("executing: {}", info.getSql());
            LOGGER.debug("parsed: {}", new ObjectMapper().writeValueAsString(addParameterJpa(query, info)));

            if (!info.isJavaObject() && !info.isEntity()) {
                if (info.isUseHibernateTypes()) {
                    HibernateTypesMapper.map(query, info.getAliasToBean());
                }
                query.setResultTransformer(Transformers.aliasToBean(info.getAliasToBean()));
            }

            if (info.returnTypeIsListResult()) {
                DaoQueryListResult result = new DaoQueryListResult<>();
                result.setData(processReturn(query.list()));

                query = session.createNativeQuery(info.getSqlCount(), Integer.class);
                addParameterJpa(query, info, DaoQueryTemplateDataType.COUNT);
                result.setCount((Integer) query.getSingleResultOrNull());
                return result;
            }

            if (info.getReturnType().getSimpleName().equals(Void.TYPE.getName())) {
                query.executeUpdate();
                return null;
            }

            if (info.returnTypeIsOptional()) {
                return getOptionalReturn(query::getSingleResult);
            }

            if (info.isSingleResult()) {

                if (info.hasSqlReturn()) {
                    query.executeUpdate();

                    query = session.createNativeQuery(info.getSqlReturn(), info.getAliasToBean());
                    addParameterJpa(query, info, DaoQueryTemplateDataType.RETURN);
                }

                Object obj = query.getSingleResultOrNull();
                if (obj == null)
                    return null;
                else if (obj instanceof DaoQueryDomain cln)
                    return cln.clone();
                else
                    return obj;
            }

            return processReturn(query.list());
        } finally {
            entityManager.close();
            session.close();
        }
    }

    private Object getOptionalReturn(Supplier<Object> result) {
        try {
            return Optional.ofNullable(result.get());
        } catch (NoResultException | EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private Map<String, Object> addParameterJpa(Query<?> query, DaoQueryInfo info, DaoQueryTemplateDataType templateDataType) {
        Map<String, Object> parametroList = new HashMap<>();
        info.getParameterList().forEach(parameter -> {
            try {
                if (parameter.getValue() != null && info.hasSqlParameter(parameter.getName(), templateDataType)) {
                    query.setParameter(parameter.getName(), parameter.getValue());
                    parametroList.put(parameter.getName(), parameter.getValue());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return parametroList;
    }

    private Map<String, Object> addParameterJpa(Query<?> query, DaoQueryInfo info) {
        return addParameterJpa(query, info, DaoQueryTemplateDataType.QUERY);
    }

    private <T> List<T> processReturn(List<T> nlst) {
        return (List<T>) nlst.stream().map(x-> {
            if (x instanceof DaoQueryDomain cln)
                return cln.clone();
            else
                return x;
        }).collect(Collectors.toList());
    }
}

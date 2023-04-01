package org.r3al.springdao;

import jakarta.persistence.Entity;
import org.aopalliance.intercept.MethodInvocation;
import org.r3al.springdao.annotations.*;
import org.r3al.springdao.filters.DaoQueryCondition;
import org.r3al.springdao.filters.DaoQueryFilter;
import org.r3al.springdao.templates.HandleBarTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.TypeInformation;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DaoQueryInfo implements Serializable, Cloneable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryInfo.class);

    private List<DaoQueryParameter> parameterList;
    private Class<?> aliasToBean;

    private Class<?> returnType;

    private boolean returnTypeIsIterable;
    private boolean useJdbcTemplate;
    private boolean useHibernateTypes;
    private Boolean isEntity;
    private String sql;

    private DaoQuerySqlPattern sqlPattern;

    private boolean useSqlInline;
    private String sqlCount;
    private String sqlReturn;

    private RowMapper<?> rowMapper;
    private boolean useRowMapper;

    private DaoQueryInfo() {
    }

    public static DaoQueryInfo of(Class<? extends DaoQuery> classe, MethodInvocation invocation) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        DaoQueryInfo info = new DaoQueryInfo();
        Method method = invocation.getMethod();
        LOGGER.debug("invoked method {}", method.getName());
        info.useSqlInline = method.isAnnotationPresent(DaoQuerySql.class);
        if (info.useSqlInline) {
            info.sql = method.getAnnotation(DaoQuerySql.class).value();
        }
        info.returnType = method.getReturnType();
        info.returnTypeIsIterable = Iterable.class.isAssignableFrom(info.returnType);
        if (info.returnTypeIsIterable || info.returnTypeIsOptional()) {
            TypeInformation<?> componentType = TypeInformation.fromReturnTypeOf(method).getComponentType();
            info.aliasToBean = Objects.requireNonNull(componentType).getType();
        } else if (info.returnTypeIsListResult()) {
            info.aliasToBean = (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        } else {
            info.aliasToBean = info.returnType;
        }

        info.useRowMapper = info.aliasToBean.isAnnotationPresent(DaoQueryRowMapper.class);
        if (info.useRowMapper) {
            info.rowMapper = info.aliasToBean.getAnnotation(DaoQueryRowMapper.class).mapper().getDeclaredConstructor().newInstance();
        }

        if (method.isAnnotationPresent(DaoQueryUseHibernateTypes.class)) {
            info.useHibernateTypes = method.getAnnotation(DaoQueryUseHibernateTypes.class).useHibernateTypes();
        } else {
            info.useHibernateTypes = Boolean.parseBoolean(PropertyUtil.getValue("dao-query.use-hibernate-types", "true"));
        }

        info.useJdbcTemplate = method.isAnnotationPresent(DaoQueryUseJdbcTemplate.class);
        if (info.useJdbcTemplate) {
            DaoQueryUseJdbcTemplate jdbcTemplate = method.getAnnotation(DaoQueryUseJdbcTemplate.class);
        } else {
            info.useJdbcTemplate = Boolean.parseBoolean(PropertyUtil.getValue("dao-query.use-jdbc", "false"));
        }

        return info;
    }

    public static void setParameters(DaoQueryInfo info, MethodInvocation invocation) {
        info.sql = null;
        info.parameterList = new ArrayList<>();
        info.parameterList.add(info.parseDbType(PropertyUtil.getValue("spring.datasource.url", "jdbc:unknown")));

        for (int i = 0; i < invocation.getArguments().length; i++) {
            Object argument = invocation.getArguments()[i];
            Parameter parameter = invocation.getMethod().getParameters()[i];

            if (parameter.getType().isAssignableFrom(DaoQueryListToken.class)) {
                var token = (DaoQueryListToken) argument;
                info.parameterList.add(new DaoQueryParameter("skip", token.getSkip()));
                info.parameterList.add(new DaoQueryParameter("take", token.getTake()));
            } else {
                if (parameter.isAnnotationPresent(DaoQueryParam.class)) {
                    DaoQueryParam param = parameter.getAnnotation(DaoQueryParam.class);

                    if (param.addChildren()) {
                        info.parameterList.addAll(DaoQueryParameter.ofDeclaredMethods(param.value(), parameter.getType(), argument));
                    } else {
                        if (argument instanceof Map map) {
                            info.parameterList.addAll(DaoQueryParameter.ofMap(map, param.value()));
                        }
                        if (argument instanceof Enum) {
                            info.parameterList.add(new DaoQueryParameter(param.value(), DaoQueryParameter.getEnumValue(argument)));
                        } else {
                            info.parameterList.add(new DaoQueryParameter(param.value(), argument));
                        }
                    }
                } else {
                    if (argument instanceof Map map) {
                        info.parameterList.addAll(DaoQueryParameter.ofMap(map, parameter.getName()));
                    } else if (argument instanceof Enum) {
                        info.parameterList.add(new DaoQueryParameter(parameter.getName(), DaoQueryParameter.getEnumValue(argument)));
                    } else if (argument instanceof org.r3al.springdao.filters.DaoQuerySql sql) {
                        info.parameterList.add(new DaoQueryParameter(parameter.getName(), sql.getSql()));
                        info.parameterList.addAll(sql.getParameters());
                    } else if (argument instanceof DaoQueryCondition condition) {
                        info.parameterList.add(new DaoQueryParameter(parameter.getName(), condition.getSql()));
                        info.parameterList.addAll(condition.getParameters());
                    } else {
                        info.parameterList.add(new DaoQueryParameter(parameter.getName(), argument));
                    }
                }
            }
        }
    }

    public String getSql() throws IOException {

        if (useSqlInline || sql != null) {
            return sql;
        }

        if (sqlPattern.templateLanguage.equals("hbs")) {
            Map<String, Object> nmap = this.getParameterMap();
            nmap.put("queryType", DaoQueryTemplateDataType.QUERY);
            sql = new HandleBarTemplate().process(sqlPattern.template, nmap);
        } else {
            sql = sqlPattern.template;
        }

        return sql;
    }

    public String getSqlCount() throws IOException {
        if (sqlCount != null) {
            return sqlCount;
        }
        if (useSqlInline) {
            throw new RuntimeException("Inline Sql is not supported when getting query count");
        }

        Map<String, Object> nmap = this.getParameterMap();
        nmap.put("queryType", DaoQueryTemplateDataType.COUNT);
        sqlCount = new HandleBarTemplate().process(sqlPattern.template, nmap);
        return sqlCount;
    }

    public boolean hasSqlCount() throws IOException {
        return getSqlCount() != null && !getSqlCount().isEmpty();
    }

    public String getSqlReturn() throws IOException {
        if (sqlReturn != null) {
            return sqlReturn;
        }
        if (useSqlInline) {
            throw new RuntimeException("Inline Sql is not supported when getting query return");
        }

        if (!sqlPattern.template.contains("{{#return")) {
            sqlReturn = "";
        } else {
            Map<String, Object> nmap = this.getParameterMap();
            nmap.put("queryType", DaoQueryTemplateDataType.RETURN);
            sqlReturn = new HandleBarTemplate().process(sqlPattern.template, nmap);
        }
        return sqlReturn;
    }

    public boolean hasSqlReturn() throws IOException {
        return getSqlReturn() != null && !getSqlReturn().isEmpty();
    }

    public void setSqlPattern(DaoQuerySqlPattern sqlPattern) {
        this.sqlPattern = sqlPattern;
    }

    public boolean isJavaObject() {
        return getPackageName(aliasToBean).startsWith("java");
    }

    public boolean isEntity() {
        if (isEntity == null) {
            isEntity = aliasToBean.isAnnotationPresent(Entity.class);
        }
        return isEntity;
    }

    private String getPackageName(Class<?> c) {
        final String pn;
        while (c.isArray()) {
            c = c.getComponentType();
        }
        if (c.isPrimitive()) {
            pn = "java.lang";
        } else {
            String cn = c.getName();
            int dot = cn.lastIndexOf('.');
            pn = (dot != -1) ? cn.substring(0, dot).intern() : "";
        }
        return pn;
    }

    public boolean isSingleResult() {
        return !returnTypeIsIterable;
    }

    public boolean isUseSqlInline() {
        return useSqlInline;
    }

    public List<DaoQueryParameter> getParameterList() {
        return this.parameterList;
    }

    public Class<?> getAliasToBean() {
        return this.aliasToBean;
    }

    public Class<?> getReturnType() {
        return this.returnType;
    }

    public RowMapper<?> getRowMapper() {
        return this.rowMapper;
    }

    public boolean isUseRowMapper() {
        return this.useRowMapper;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean returnTypeIsOptional() {
        return this.returnType.getSimpleName().equals(Optional.class.getSimpleName());
    }

    public boolean returnTypeIsListResult() {
        return this.returnType.getName().equals(DaoQueryListResult.class.getName());
    }

    private DaoQueryParameter parseDbType(String jdbcUrl) {
        final String regex = "\\:([a-zA-Z]+)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(jdbcUrl);

        if (!matcher.find()) {
            throw new RuntimeException("Couldn't identify database type");
        }
        return new DaoQueryParameter("dbType", matcher.group(1));
    }

    public String getSqlPattern() {
        return this.sqlPattern.template;
    }

    public String getSqlKey() {
        return this.sqlPattern.getKey();
    }

    private Map<String, Object> getParameterMap() {
        return this.parameterList.stream().collect(Collectors.toMap(DaoQueryParameter::getName, DaoQueryParameter::getValue));
    }

    public boolean isUseJdbcTemplate() {
        return useJdbcTemplate;
    }

    public boolean isUseHibernateTypes() {
        return useHibernateTypes;
    }

    public boolean hasSqlParameter(String name, DaoQueryTemplateDataType templateDataType) throws IOException {
        final String regex = String.format("(\\:\\b%s\\b)(;?)", name);
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

        String sql = getSql();
        if (templateDataType == DaoQueryTemplateDataType.RETURN) sql = getSqlReturn();
        else if (templateDataType == DaoQueryTemplateDataType.COUNT) sql = getSqlCount();

        final Matcher matcher = pattern.matcher(sql);
        return matcher.find();
    }
}

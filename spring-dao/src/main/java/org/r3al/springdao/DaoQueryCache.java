package org.r3al.springdao;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.convert.ConversionService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DaoQueryCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryCache.class);
    private static volatile String CACHE_ROOT_PACKAGE;
    private static final Map<DaoQueryInfoKey, DaoQueryInfo> CACHE_Dao_QUERY_INFO = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, DaoQueryFieldInfo>> CACHE_FIELD_INFO = new ConcurrentHashMap<>();
    private static final Map<String, List<DaoQueryAccessMethod>> CACHE_ACCESS_METHODS = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, DaoQuerySqlPattern>> CACHE_SQL_PATTERN = new ConcurrentHashMap<>();

    private static volatile ConversionService converter;

    public static DaoQueryInfo get(Class<? extends DaoQuery> classe, MethodInvocation invocation) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        DaoQueryInfoKey DaoQueryInfoKey = new DaoQueryInfoKey(
                classe.getName(),
                invocation.getMethod().getName()
        );

        DaoQueryInfo cached = DaoQueryCache.CACHE_Dao_QUERY_INFO.get(DaoQueryInfoKey);
        if (cached == null) {
            DaoQueryInfo built = DaoQueryInfo.of(classe, invocation);
            cached = DaoQueryCache.CACHE_Dao_QUERY_INFO.putIfAbsent(DaoQueryInfoKey, built);
            if (cached == null) {
                cached = built;
            }
        }
        DaoQueryInfo info;
        try {
            info = (DaoQueryInfo) cached.clone();
        } catch (CloneNotSupportedException e) {
            LOGGER.debug("error in cloning the information that was cached in method {} of class {}", invocation.getMethod().getName(), classe.getName());
            throw new RuntimeException(e);
        }

        if (!info.isUseSqlInline()) {
            Map<String, DaoQuerySqlPattern> sqlPatternMap = CACHE_SQL_PATTERN.get(classe.getName());
            if (sqlPatternMap == null) {
                DaoQueryResourceLoader resourceLoader = new DaoQueryResourceLoader(getBootApplicationPackageName());
                List<DaoQuerySqlPattern> sqlPatterns = resourceLoader.loadResource(classe);
                Map<String, DaoQuerySqlPattern> built = sqlPatterns.stream().collect(Collectors.toMap(DaoQuerySqlPattern::getKey, Function.identity()));
                sqlPatternMap = DaoQueryCache.CACHE_SQL_PATTERN.putIfAbsent(classe.getName(), built);
                if (sqlPatternMap == null) {
                    sqlPatternMap = built;
                }
            }
            DaoQuerySqlPattern pattern = sqlPatternMap.get(invocation.getMethod().getName());
            if (pattern == null) {
                throw new RuntimeException("SQL id " + invocation.getMethod().getName() + " does not exists in resource");
            }
            info.setSqlPattern(pattern);
        }

        DaoQueryInfo.setParameters(info, invocation);
        return info;
    }

    public static ConversionService getConverter() {
        ConversionService local = converter;
        if (local == null) {
            local = ApplicationContextProvider.getApplicationContext().getBean(ConversionService.class);
            converter = local;
        }
        return local;
    }

    static List<DaoQueryAccessMethod> getAccessMethods(Class<?> classe) {
        return CACHE_ACCESS_METHODS.computeIfAbsent(classe.getName(), className -> {
            List<DaoQueryAccessMethod> methods = new ArrayList<>();
            for (Method method : classe.getDeclaredMethods()) {
                if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                    methods.add(new DaoQueryAccessMethod(method));
                }
            }
            return methods;
        });
    }

    static Map<String, DaoQueryFieldInfo> getFieldInfo(Class<?> classe) {
        return CACHE_FIELD_INFO.computeIfAbsent(classe.getName(), className -> {
            Map<String, DaoQueryFieldInfo> fieldInfoMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            List<DaoQueryAccessField> accessFields = getAccessFields(classe);
            for (DaoQueryAccessField accessField : accessFields) {
                fieldInfoMap.put(accessField.getName(), new DaoQueryFieldInfo(accessField.getName(), accessField.getParam(), accessField.getType(), accessField.getColumn()));
            }

            return fieldInfoMap;
        });
    }

    private static List<DaoQueryAccessField> getAccessFields(Class<?> classe) {
        List<DaoQueryAccessField> fields = new ArrayList<>();
        for (Field field : classe.getDeclaredFields()) {
            //Dao Domains should not have companion objects, thus this will reflect in java declared fields
            fields.add(new DaoQueryAccessField(field));
        }
        return fields;
    }

    private static String getBootApplicationPackageName() {
        String local = CACHE_ROOT_PACKAGE;
        if (local == null) {
            Map<String, Object> candidates = ApplicationContextProvider.getApplicationContext().getBeansWithAnnotation(SpringBootApplication.class);
            local = candidates.values().toArray()[0].getClass().getPackageName();
            CACHE_ROOT_PACKAGE = local;
        }
        return local;
    }

    private static class DaoQueryInfoKey {

        String className;

        String methodName;

        public DaoQueryInfoKey(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DaoQueryInfoKey DaoQueryInfoKey = (DaoQueryInfoKey) o;
            return Objects.equals(className, DaoQueryInfoKey.className) &&
                    Objects.equals(methodName, DaoQueryInfoKey.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, methodName);
        }

        @Override
        public String toString() {
            return "DaoQueryInfoKey{" +
                    "className='" + className + '\'' +
                    ", methodName='" + methodName + '\'' +
                    '}';
        }
    }

}

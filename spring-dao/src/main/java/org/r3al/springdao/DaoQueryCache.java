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
import java.util.function.Function;
import java.util.stream.Collectors;

public class DaoQueryCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryCache.class);
    private static String CACHE_ROOT_PACKAGE;
    private static final Map<DaoQueryInfoKey, DaoQueryInfo> CACHE_Dao_QUERY_INFO = new HashMap<>();
    private static final Map<String, Map<String, DaoQueryFieldInfo>> CACHE_FIELD_INFO = new HashMap<>();
    private static final Map<String, List<DaoQueryAccessMethod>> CACHE_ACCESS_METHODS = new HashMap<>();
    private static final Map<String, Map<String, DaoQuerySqlPattern>> CACHE_SQL_PATTERN = new HashMap<>();

    private static ConversionService converter;

    public static DaoQueryInfo get(Class<? extends DaoQuery> classe, MethodInvocation invocation) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        DaoQueryInfoKey DaoQueryInfoKey = new DaoQueryInfoKey(
                classe.getName(),
                invocation.getMethod().getName()
        );

        DaoQueryInfo info = DaoQueryCache.CACHE_Dao_QUERY_INFO.get(DaoQueryInfoKey);
        if (info == null) {
            info = DaoQueryInfo.of(classe, invocation);
            DaoQueryCache.CACHE_Dao_QUERY_INFO.put(DaoQueryInfoKey, info);
        } else {
            try {
                info = (DaoQueryInfo) info.clone();
            } catch (CloneNotSupportedException e) {
                LOGGER.debug("error in cloning the information that was cached in method {} of class {}", invocation.getMethod().getName(), classe.getName());
                throw new RuntimeException(e);
            }
        }

        if (!info.isUseSqlInline()) {
            Map<String, DaoQuerySqlPattern> sqlPatternMap = CACHE_SQL_PATTERN.get(classe.getName());
            if (sqlPatternMap == null) {
                DaoQueryResourceLoader resourceLoader = new DaoQueryResourceLoader(getBootApplicationPackageName());
                List<DaoQuerySqlPattern> sqlPatterns = resourceLoader.loadResource(classe);
                sqlPatternMap = sqlPatterns.stream().collect(Collectors.toMap(DaoQuerySqlPattern::getKey, Function.identity()));
                DaoQueryCache.CACHE_SQL_PATTERN.put(classe.getName(), sqlPatternMap);
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
        if (converter == null) {
            converter = ApplicationContextProvider.getApplicationContext().getBean(ConversionService.class);
        }
        return converter;
    }

    static List<DaoQueryAccessMethod> getAccessMethods(Class<?> classe) {
        String className = classe.getName();
        List<DaoQueryAccessMethod> methods = CACHE_ACCESS_METHODS.get(className);
        if (methods == null) {
            methods = new ArrayList<>();
            for (Method method : classe.getDeclaredMethods()) {
                if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                    methods.add(new DaoQueryAccessMethod(method));
                }
            }
            CACHE_ACCESS_METHODS.put(className, methods);
        }
        return methods;
    }

    static Map<String, DaoQueryFieldInfo> getFieldInfo(Class<?> classe) {
        String className = classe.getName();
        Map<String, DaoQueryFieldInfo> fieldInfoMap = CACHE_FIELD_INFO.get(className);
        if (fieldInfoMap == null) {
            fieldInfoMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            List<DaoQueryAccessField> accessFields = getAccessFields(classe);
            for (DaoQueryAccessField accessField : accessFields) {
                fieldInfoMap.put(accessField.getName(), new DaoQueryFieldInfo(accessField.getName(), accessField.getParam(), accessField.getType(), accessField.getColumn()));
            }

//            List<DaoQueryAccessMethod> accessMethods = getAccessMethods(classe);
//            for (DaoQueryAccessMethod accessMethod : accessMethods) {
//                if (fieldInfoMap.get(accessMethod.getName()) == null) {
//                    fieldInfoMap.put(accessMethod.getName(), new DaoQueryFieldInfo(accessMethod.getParam(), accessMethod.getType(), null));
//                }
//            }

            CACHE_FIELD_INFO.put(className, fieldInfoMap);
        }
        return fieldInfoMap;
    }

    private static List<DaoQueryAccessField> getAccessFields(Class<?> classe) {
        List<DaoQueryAccessField> fields = new ArrayList<>();
        for (Field field : classe.getDeclaredFields()) {
            if (Arrays.stream(field.getType().getAnnotations()).anyMatch(x -> x.annotationType().getName().equals("kotlin.Metadata"))) {
                continue;
            }

            fields.add(new DaoQueryAccessField(field));
        }
        return fields;
    }

    private static String getBootApplicationPackageName() {
        if (CACHE_ROOT_PACKAGE == null) {
            Map<String, Object> candidates = ApplicationContextProvider.getApplicationContext().getBeansWithAnnotation(SpringBootApplication.class);
            CACHE_ROOT_PACKAGE = candidates.values().toArray()[0].getClass().getPackageName();
        }
        return CACHE_ROOT_PACKAGE;
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

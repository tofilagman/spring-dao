package org.r3al.springdao;

import org.apache.commons.text.WordUtils;
import org.r3al.springdao.annotations.DaoQueryParam;
import org.r3al.springdao.filters.DaoQueryCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.util.TypeInformation;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class DaoQueryParameter implements Serializable, Cloneable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryParameter.class);

    private final String name;

    private final Object value;

    public DaoQueryParameter(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    static List<DaoQueryParameter> ofDeclaredMethods(String parentName, Class<?> classe, Object object) {
        ArrayList<DaoQueryParameter> parameterList = new ArrayList<>();

        Map<String, DaoQueryFieldInfo> fieldInfoMap = DaoQueryCache.getFieldInfo(classe);
        List<DaoQueryAccessMethod> accessMethods = DaoQueryCache.getAccessMethods(classe);
        for (DaoQueryAccessMethod accessMethod : accessMethods) {
            Object value = getValue(object, accessMethod.getMethod());

            DaoQueryFieldInfo fieldInfo = fieldInfoMap.get(accessMethod.getName());
            if (fieldInfo != null) {
                DaoQueryParam queryParam = fieldInfo.getParam() != null ? fieldInfo.getParam() : accessMethod.getParam();
                if (queryParam != null) {
                    if (queryParam.addChildren()) {
                        String parentNameChildren = parentName + WordUtils.capitalize(queryParam.value());
                        parameterList.addAll(ofDeclaredMethods(parentNameChildren, fieldInfo.getType(), value));
                    } else {
                        String paramName = parentName + WordUtils.capitalize(queryParam.value());
                        if (value instanceof Map) {
                            parameterList.addAll(ofMap((Map) value, paramName));
                        } else {
                            parameterList.add(new DaoQueryParameter(paramName, value));
                        }
                    }
                } else {
                    if (value instanceof Map) {
                        parameterList.addAll(ofMap((Map) value, parentName + accessMethod.getName()));
                    } else {
                        Object paramValue = DaoQueryOperator.DEFAULT.getTransformParam().apply(value);
                        parameterList.add(new DaoQueryParameter(parentName + accessMethod.getName(), paramValue));
                    }
                }
            }
        }

        return parameterList;
    }

    static List<DaoQueryParameter> ofDeclaredMethods(Class<?> classe, Object object) {
        ArrayList<DaoQueryParameter> parameterList = new ArrayList<>();

        Map<String, DaoQueryFieldInfo> fieldInfoMap = DaoQueryCache.getFieldInfo(classe);
        List<DaoQueryAccessMethod> accessMethods = DaoQueryCache.getAccessMethods(classe);
        for (DaoQueryAccessMethod accessMethod : accessMethods) {
            Object value = getValue(object, accessMethod.getMethod());
            DaoQueryFieldInfo fieldInfo = fieldInfoMap.get(accessMethod.getName());

            if (fieldInfo != null) {
                String fieldName = fieldInfo.getSqlName();

                if (value == null) {
                    parameterList.add(new DaoQueryParameter(fieldName, null));
                    continue;
                }

                if (value instanceof Boolean bool) {
                    parameterList.add(new DaoQueryParameter(fieldName, bool));
                } else if (value instanceof Enum) {
                    parameterList.add(new DaoQueryParameter(fieldName, DaoQueryParameter.getEnumValue(value)));
                } else if (value instanceof Collection<?> lst) {
                    parameterList.add(new DaoQueryParameter(fieldName, DaoQueryParameter.getList(lst)));
                } else if (!DaoQueryInfo.getPackageName(value.getClass()).startsWith("java")) {
                    parameterList.addAll(DaoQueryParameter.ofDeclaredMethods(fieldInfo.getType(), value));
                } else {
                    parameterList.add(new DaoQueryParameter(fieldName, value));
                }
            }
        }

        return parameterList;
    }

    public static Object getEnumValue(Object obj) {
        ConversionService converter = DaoQueryCache.getConverter();
        return converter.convert(obj, Integer.class);
    }

    private static Object getValue(Object object, Method method) {
        try {
            return method.invoke(object);
        } catch (Exception ignore) {
            return null;
        }
    }

    public static Collection<?> getList(Collection<?> objects) {
        if (objects.isEmpty()) {
            List<Object> obj = new ArrayList<>();
            obj.add(null);
            return obj;
        }

        return objects.stream().map(x -> {
            if (x instanceof Enum) {
                return getEnumValue(x);
            } else
                return x;
        }).collect(Collectors.toList());
    }

    static List<DaoQueryParameter> ofMap(Map map, String name) {
        ArrayList<DaoQueryParameter> parameterList = new ArrayList<>();
        parameterList.add(new DaoQueryParameter(name, map.keySet()));
        map.forEach((k, v) -> parameterList.add(new DaoQueryParameter(k.toString(), v)));
        return parameterList;
    }

    public String getName() {
        return this.name;
    }

    public Object getValue() {
        return this.value;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "DaoQueryParameter{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}

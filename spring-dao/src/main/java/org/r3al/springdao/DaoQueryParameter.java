package org.r3al.springdao;

import org.apache.commons.text.WordUtils;
import org.r3al.springdao.annotations.DaoQueryParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

                DaoQueryParam queryParam = fieldInfo.getParam() != null ? fieldInfo.getParam() : accessMethod.getParam();
                if (queryParam != null) {
                    if (queryParam.addChildren()) {
                        parameterList.addAll(ofDeclaredMethods(queryParam.value(), fieldInfo.getType(), value));
                    } else {
                        if (value instanceof Map) {
                            parameterList.addAll(ofMap((Map) value, queryParam.value()));
                        } else if (value instanceof Enum) {
                            parameterList.add(new DaoQueryParameter(queryParam.value(), getEnumValue(value)));
                        } else {
                            parameterList.add(new DaoQueryParameter(queryParam.value(), value));
                        }
                    }
                } else {
                    if (value instanceof Map) {
                        parameterList.addAll(ofMap((Map) value, fieldName));
                    } else if (value instanceof Enum) {
                        parameterList.add(new DaoQueryParameter(fieldName, getEnumValue(value)));
                    } else {
                        parameterList.add(new DaoQueryParameter(fieldName, value));
                    }
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

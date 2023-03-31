package org.r3al.springdao;

import jakarta.persistence.Column;
import org.r3al.springdao.annotations.DaoQueryParam;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

public class DaoQueryAccessMethod {

    private final Method method;

    private final String name;

    private final Class<?> type;

    private   Column column ;

    private DaoQueryParam param;

    public DaoQueryAccessMethod(Method method) {
        this.method = method;
        this.name = method.getName().substring(method.getName().startsWith("get") ? 3 : 2);
        if (method.getAnnotatedReturnType().getType() instanceof ParameterizedType) {
            this.type = (Class<?>) ((ParameterizedType) method.getAnnotatedReturnType().getType()).getRawType();
        } else {
            this.type = (Class<?>) method.getAnnotatedReturnType().getType();
        }
        if (method.isAnnotationPresent(DaoQueryParam.class)) {
            this.param = method.getAnnotation(DaoQueryParam.class);
        }
        if (method.isAnnotationPresent(Column.class)) {
            this.column = method.getAnnotation(Column.class);
        }
    }

    public Method getMethod() {
        return method;
    }

    public String getName() {
        return name;
    }

    public DaoQueryParam getParam() {
        return param;
    }

    public Class<?> getType() {
        return type;
    }

    public Column getColumn() {
        return column;
    }

    public String getSqlName() {
        if(column != null)
            return column.name();
        else
            return name;
    }
}

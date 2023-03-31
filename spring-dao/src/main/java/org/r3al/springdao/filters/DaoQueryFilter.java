package org.r3al.springdao.filters;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class DaoQueryFilter<T> {
    public abstract void build(DaoQueryCondition condition, T data);

    public Class<T> INSTANCE(){
        Type type = getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) type;
        return (Class<T>) parameterizedType.getActualTypeArguments()[0];
    }
}

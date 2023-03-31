package org.r3al.springdao;

import jakarta.persistence.Column;
import org.r3al.springdao.annotations.DaoQueryParam;

import java.lang.reflect.Field;

public class DaoQueryAccessField {

    private final String name;

    private final Class<?> type;

    private DaoQueryParam param;

    private Column column;

    public DaoQueryAccessField(Field field) {
        this.name = field.getName();
        this.type = field.getType();
        if (field.isAnnotationPresent(DaoQueryParam.class)) {
            this.param = field.getAnnotation(DaoQueryParam.class);
        }
        if(field.isAnnotationPresent(Column.class)) {
            this.column = field.getAnnotation(Column.class);
        }
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public DaoQueryParam getParam() {
        return param;
    }

    public Column getColumn(){
        return column;
    }

    public String getSqlName(){
        if(this.column != null)
            return this.column.name();
        else
            return this.name;
    }
}

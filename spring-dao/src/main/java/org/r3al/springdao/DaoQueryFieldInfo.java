package org.r3al.springdao;

import jakarta.persistence.Column;
import org.r3al.springdao.annotations.DaoQueryParam;

public class DaoQueryFieldInfo {

    private final DaoQueryParam param;
    private final  Class<?> type;
    private final String name;
    private final Column column;

    public DaoQueryFieldInfo(String name, DaoQueryParam param, Class<?> type, Column column) {
        this.name = name;
        this.param = param;
        this.type = type;
        this.column = column;
    }

    public DaoQueryParam getParam() {
        return param;
    }

    public Class<?> getType() {
        return type;
    }

    public Column getColumn() { return column; }

    public String getSqlName(){
        if(column != null)
            return column.name();
        else
            return name;
    }

    public String getName() {
        return name;
    }
}

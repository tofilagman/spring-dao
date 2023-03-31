package org.r3al.springdao.mapper;

import java.util.List;

public class DaoQueryMapItem {
    private String column;

    public String getColumn() {
        return column;
    }

    public DaoQueryMapOperator getOperator() {
        return operator;
    }

    public List<Object> getValues() {
        return values;
    }

    private DaoQueryMapOperator operator;
    private List<Object> values;

    public DaoQueryMapItem(String column, DaoQueryMapOperator operator, List<Object> values) {
        this.column = column;
        this.operator = operator;
        this.values = values;
    }
}

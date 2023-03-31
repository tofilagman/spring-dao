package org.r3al.springdao.mapper;

public enum DaoQueryMapOperator {
    EQ("="),
    NE("<>"),
    GT(">"),
    LT("<"),
    GTE(">="),
    LTE("<="),
    BETWEEN(""),
    NOTBETWEEN(""),
    IN(""),
    NOTIN(""),
    ISNULL(""),
    ISNOTNULL("");

    private String operator;

    DaoQueryMapOperator(String operator) {
        this.operator = operator;
    }
}

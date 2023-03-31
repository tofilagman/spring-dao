package org.r3al.springdao.filters;

public class DaoQueryConditionItem {

    public String sql;
    public DaoQueryConditionOperator op;

    public DaoQueryConditionItem(String sql, DaoQueryConditionOperator op) {
        this.sql = sql;
        this.op = op;
    }

   public static DaoQueryConditionItem INSTANCE(String sql, DaoQueryConditionOperator op) {
        return new DaoQueryConditionItem(sql, op);
    }
}

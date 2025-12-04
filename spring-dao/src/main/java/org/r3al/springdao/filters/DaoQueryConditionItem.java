package org.r3al.springdao.filters;

public class DaoQueryConditionItem {

    public String sql;
    public DaoQueryConditionOperator op;
    public DaoQueryCondition condition;

    public DaoQueryConditionItem(String sql, DaoQueryConditionOperator op) {
        this.sql = sql;
        this.op = op;
    }

    public DaoQueryConditionItem(DaoQueryCondition condition, DaoQueryConditionOperator op) {
        this.condition = condition;
        this.op = op;
    }

   public static DaoQueryConditionItem INSTANCE(String sql, DaoQueryConditionOperator op) {
        return new DaoQueryConditionItem(sql, op);
    }

    public static DaoQueryConditionItem INSTANCE(DaoQueryCondition condition, DaoQueryConditionOperator op) {
        return new DaoQueryConditionItem(condition, op);
    }
}

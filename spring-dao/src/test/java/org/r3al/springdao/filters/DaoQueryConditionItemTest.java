package org.r3al.springdao.filters;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryConditionItemTest {

    @Test
    void sqlConstructorPopulatesSqlAndOperator() {
        DaoQueryConditionItem item = new DaoQueryConditionItem("a = 1", DaoQueryConditionOperator.AND);

        assertThat(item.sql).isEqualTo("a = 1");
        assertThat(item.op).isEqualTo(DaoQueryConditionOperator.AND);
        assertThat(item.condition).isNull();
    }

    @Test
    void conditionConstructorPopulatesNestedConditionAndOperator() {
        DaoQueryCondition nested = DaoQueryCondition.INSTANCE();
        DaoQueryConditionItem item = new DaoQueryConditionItem(nested, DaoQueryConditionOperator.OR);

        assertThat(item.condition).isSameAs(nested);
        assertThat(item.op).isEqualTo(DaoQueryConditionOperator.OR);
        assertThat(item.sql).isNull();
    }

    @Test
    void sqlFactoryProducesEquivalentInstance() {
        DaoQueryConditionItem item = DaoQueryConditionItem.INSTANCE("x = 0", DaoQueryConditionOperator.AND);

        assertThat(item.sql).isEqualTo("x = 0");
        assertThat(item.op).isEqualTo(DaoQueryConditionOperator.AND);
    }

    @Test
    void conditionFactoryProducesEquivalentInstance() {
        DaoQueryCondition nested = DaoQueryCondition.INSTANCE();
        DaoQueryConditionItem item = DaoQueryConditionItem.INSTANCE(nested, DaoQueryConditionOperator.OR);

        assertThat(item.condition).isSameAs(nested);
        assertThat(item.op).isEqualTo(DaoQueryConditionOperator.OR);
    }
}

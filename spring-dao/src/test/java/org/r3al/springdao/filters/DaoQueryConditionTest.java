package org.r3al.springdao.filters;

import org.junit.jupiter.api.Test;
import org.r3al.springdao.DaoQueryParameter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryConditionTest {

    @Test
    void emptyConditionReturnsAlwaysTrue() {
        DaoQueryCondition c = DaoQueryCondition.INSTANCE();

        assertThat(c.getSql()).isEqualTo("1=1");
        assertThat(c.getParameters()).isEmpty();
    }

    @Test
    void singleAndYieldsParenthesizedFragmentWithNamedParam() {
        DaoQueryCondition c = DaoQueryCondition.INSTANCE();
        c.and("name = $name", "alice");

        String sql = c.getSql();
        assertThat(sql).contains("name = :FLD").doesNotContain("$name");

        Map<String, Object> map = paramsAsMap(c);
        assertThat(map).hasSize(1);
        assertThat(map.values()).contains("alice");
    }

    @Test
    void andAndOrChainComposeWithOperatorsBetweenClauses() {
        DaoQueryCondition c = DaoQueryCondition.INSTANCE();
        c.and("a = $a", 1);
        c.or("b = $b", 2);
        c.and("c = $c", 3);

        String sql = c.getSql();
        assertThat(sql).contains(" OR ").contains(" AND ");

        Map<String, Object> map = paramsAsMap(c);
        Collection<Object> values = map.values();
        assertThat(values).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void conditionTypeLikeWrapsValueInPercentSigns() {
        DaoQueryCondition c = DaoQueryCondition.INSTANCE();
        c.and("name like $name", "alice", DaoQueryConditionType.LIKE);

        Object value = paramsAsMap(c).values().iterator().next();
        assertThat(value).isEqualTo("%alice%");
    }

    @Test
    void beginLikeAppendsTrailingPercent() {
        DaoQueryCondition c = DaoQueryCondition.INSTANCE();
        c.and("name like $name", "ali", DaoQueryConditionType.BEGIN_LIKE);

        assertThat(paramsAsMap(c).values().iterator().next()).isEqualTo("ali%");
    }

    @Test
    void endLikePrependsLeadingPercent() {
        DaoQueryCondition c = DaoQueryCondition.INSTANCE();
        c.and("name like $name", "ice", DaoQueryConditionType.END_LIKE);

        assertThat(paramsAsMap(c).values().iterator().next()).isEqualTo("%ice");
    }

    @Test
    void groupedConditionNestsInParentheses() {
        DaoQueryCondition inner = DaoQueryCondition.INSTANCE();
        inner.and("a = $a", 1);
        inner.or("b = $b", 2);

        DaoQueryCondition outer = DaoQueryCondition.INSTANCE();
        outer.and("c = $c", 3);
        outer.group(inner, DaoQueryConditionOperator.AND);

        String sql = outer.getSql();
        assertThat(sql).contains("(");

        Map<String, Object> map = paramsAsMap(outer);
        assertThat(map.values()).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void addWithExplicitOperator() {
        DaoQueryCondition c = DaoQueryCondition.INSTANCE();
        c.add("a = $a", 1, DaoQueryConditionOperator.AND);
        c.add("b = $b", 2, DaoQueryConditionOperator.OR);

        assertThat(c.getSql()).contains(" OR ");
        Collection<Object> values = paramsAsMap(c).values();
        assertThat(values).containsExactlyInAnyOrder(1, 2);
    }

    private static Map<String, Object> paramsAsMap(DaoQueryCondition c) {
        List<DaoQueryParameter> params = c.getParameters();
        return params.stream().collect(Collectors.toMap(DaoQueryParameter::getName, DaoQueryParameter::getValue));
    }
}

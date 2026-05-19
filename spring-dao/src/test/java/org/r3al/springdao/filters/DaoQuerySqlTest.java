package org.r3al.springdao.filters;

import org.junit.jupiter.api.Test;
import org.r3al.springdao.DaoQueryParameter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQuerySqlTest {

    @Test
    void singlePlaceholderRewrittenToNamedParam() {
        DaoQuerySql sql = DaoQuerySql.of("name = $name", "alice");

        assertThat(sql.getSql()).matches("name = :FQL[a-f0-9]{10}");

        List<DaoQueryParameter> params = sql.getParameters();
        assertThat(params).hasSize(1);
        assertThat(params.get(0).getValue()).isEqualTo("alice");
    }

    @Test
    void multiplePlaceholdersGetDistinctKeys() {
        DaoQuerySql sql = DaoQuerySql.of("a = $a and b = $b", 1, 2);

        List<DaoQueryParameter> params = sql.getParameters();
        assertThat(params).hasSize(2);
        Map<String, Object> values = params.stream()
                .collect(Collectors.toMap(DaoQueryParameter::getName, DaoQueryParameter::getValue));
        Collection<Object> rawValues = values.values();
        assertThat(rawValues).containsExactlyInAnyOrder(1, 2);

        // both placeholders replaced, none left
        assertThat(sql.getSql()).doesNotContain("$a").doesNotContain("$b");
        long namedParams = sql.getSql().chars().filter(c -> c == ':').count();
        assertThat(namedParams).isEqualTo(2);
    }

    @Test
    void getSqlReturnsEmptyStringWhenNeverProcessed() {
        // Edge: constructor always processes, but if user instantiates with no placeholders
        DaoQuerySql sql = DaoQuerySql.of("select 1");

        assertThat(sql.getSql()).isEqualTo("select 1");
        assertThat(sql.getParameters()).isEmpty();
    }

    @Test
    void factoryProducesEquivalentInstance() {
        DaoQuerySql viaFactory = DaoQuerySql.of("x = $x", "v");
        DaoQuerySql viaCtor = new DaoQuerySql("x = $x", "v");

        // Both have unique keys but same shape and value set
        assertThat(viaFactory.getSql()).matches("x = :FQL[a-f0-9]{10}");
        assertThat(viaCtor.getSql()).matches("x = :FQL[a-f0-9]{10}");
        assertThat(viaFactory.getParameters().get(0).getValue()).isEqualTo("v");
        assertThat(viaCtor.getParameters().get(0).getValue()).isEqualTo("v");
    }
}

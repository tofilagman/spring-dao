package org.r3al.springdao.mapper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryMapItemTest {

    @Test
    void gettersReflectConstructorArgs() {
        List<Object> values = List.of(1, 2, 3);

        DaoQueryMapItem item = new DaoQueryMapItem("user_id", DaoQueryMapOperator.IN, values);

        assertThat(item.getColumn()).isEqualTo("user_id");
        assertThat(item.getOperator()).isEqualTo(DaoQueryMapOperator.IN);
        assertThat(item.getValues()).containsExactly(1, 2, 3);
    }

    @Test
    void nullValuesAreAllowed() {
        DaoQueryMapItem item = new DaoQueryMapItem("c", DaoQueryMapOperator.ISNULL, null);

        assertThat(item.getColumn()).isEqualTo("c");
        assertThat(item.getOperator()).isEqualTo(DaoQueryMapOperator.ISNULL);
        assertThat(item.getValues()).isNull();
    }
}

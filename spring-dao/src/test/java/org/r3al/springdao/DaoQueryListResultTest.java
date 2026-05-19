package org.r3al.springdao;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryListResultTest {

    @Test
    void noArgConstructorYieldsNulls() {
        DaoQueryListResult<String> result = new DaoQueryListResult<>();

        assertThat(result.getData()).isNull();
        assertThat(result.getCount()).isNull();
    }

    @Test
    void fullConstructorPopulatesFields() {
        DaoQueryListResult<String> result = new DaoQueryListResult<>(List.of("a", "b"), 2);

        assertThat(result.getData()).containsExactly("a", "b");
        assertThat(result.getCount()).isEqualTo(2);
    }

    @Test
    void settersWork() {
        DaoQueryListResult<Integer> result = new DaoQueryListResult<>();

        result.setData(List.of(1, 2, 3));
        result.setCount(3);

        assertThat(result.getData()).containsExactly(1, 2, 3);
        assertThat(result.getCount()).isEqualTo(3);
    }

    @Test
    void countCanDifferFromDataSize() {
        DaoQueryListResult<String> result = new DaoQueryListResult<>(List.of("only one"), 9999);

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getCount()).isEqualTo(9999);
    }
}

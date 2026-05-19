package org.r3al.springdao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryOperatorTest {

    @Test
    void defaultIsIdentity() {
        assertThat(DaoQueryOperator.DEFAULT.getTransformParam().apply("hello")).isEqualTo("hello");
        assertThat(DaoQueryOperator.DEFAULT.getTransformParam().apply(42)).isEqualTo(42);
        assertThat(DaoQueryOperator.DEFAULT.getTransformParam().apply(null)).isNull();
    }
}

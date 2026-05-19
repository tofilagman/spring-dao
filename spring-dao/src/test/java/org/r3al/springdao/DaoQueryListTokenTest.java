package org.r3al.springdao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryListTokenTest {

    @Test
    void constructorSetsSkipAndTake() {
        DaoQueryListToken token = new DaoQueryListToken(10, 25);

        assertThat(token.getSkip()).isEqualTo(10);
        assertThat(token.getTake()).isEqualTo(25);
    }

    @Test
    void settersOverrideConstructorValues() {
        DaoQueryListToken token = new DaoQueryListToken(0, 0);

        token.setSkip(50);
        token.setTake(100);

        assertThat(token.getSkip()).isEqualTo(50);
        assertThat(token.getTake()).isEqualTo(100);
    }

    @Test
    void implementsTokenBase() {
        DaoQueryListToken token = new DaoQueryListToken(1, 2);

        assertThat(token).isInstanceOf(DaoQueryListTokenBase.class);
    }

    @Test
    void nullValuesAreAccepted() {
        DaoQueryListToken token = new DaoQueryListToken(null, null);

        assertThat(token.getSkip()).isNull();
        assertThat(token.getTake()).isNull();
    }
}

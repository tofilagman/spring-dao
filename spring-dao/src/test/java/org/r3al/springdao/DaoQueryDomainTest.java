package org.r3al.springdao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryDomainTest {

    static class Sample extends DaoQueryDomain {
        String name;
        int count;
    }

    @Test
    void cloneReturnsDistinctInstanceWithCopiedFields() {
        Sample original = new Sample();
        original.name = "abc";
        original.count = 7;

        DaoQueryDomain copy = original.clone();

        assertThat(copy).isNotSameAs(original);
        assertThat(copy).isInstanceOf(Sample.class);
        Sample copyAsSample = (Sample) copy;
        assertThat(copyAsSample.name).isEqualTo("abc");
        assertThat(copyAsSample.count).isEqualTo(7);
    }

    @Test
    void mutatingCloneLeavesOriginalUnchanged() {
        Sample original = new Sample();
        original.name = "orig";

        Sample copy = (Sample) original.clone();
        copy.name = "mutated";

        assertThat(original.name).isEqualTo("orig");
    }
}

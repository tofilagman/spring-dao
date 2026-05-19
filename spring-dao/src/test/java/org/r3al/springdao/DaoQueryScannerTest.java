package org.r3al.springdao;

import org.junit.jupiter.api.Test;
import org.r3al.springdao.fixtures.SampleDaoQuery;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DaoQueryScannerTest {

    @Test
    void scannerFindsDaoQuerySubInterfacesInFixturePackage() {
        Set<Class<? extends DaoQuery>> found =
                new DaoQueryScanner().scan(List.of("org.r3al.springdao.fixtures"));

        assertThat(found).contains(SampleDaoQuery.class);
    }

    @Test
    void scannerDoesNotIncludeTheMarkerInterfaceItself() {
        Set<Class<? extends DaoQuery>> found =
                new DaoQueryScanner().scan(List.of("org.r3al.springdao"));

        assertThat(found).doesNotContain(DaoQuery.class);
    }

    @Test
    void emptyPackageListYieldsEmptyResult() {
        Set<Class<? extends DaoQuery>> found = new DaoQueryScanner().scan(List.of());

        assertThat(found).isEmpty();
    }

    @Test
    void blankPackagesAreSkipped() {
        Set<Class<? extends DaoQuery>> found =
                new DaoQueryScanner().scan(List.of("", "   "));

        assertThat(found).isEmpty();
    }

    @Test
    void nonExistentPackageProducesNoResults() {
        Set<Class<? extends DaoQuery>> found =
                new DaoQueryScanner().scan(List.of("com.does.not.exist"));

        assertThat(found).isEmpty();
    }
}

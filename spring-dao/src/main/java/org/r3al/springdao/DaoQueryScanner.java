package org.r3al.springdao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Walks the given packages with Spring's own classpath scanner and returns every
 * interface that extends {@link DaoQuery}. The default scanner ignores interfaces,
 * so the candidate filter is overridden.
 */
public class DaoQueryScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryScanner.class);

    public Set<Class<? extends DaoQuery>> scan(Collection<String> packages) {
        Set<Class<? extends DaoQuery>> found = new HashSet<>();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(
                            org.springframework.beans.factory.annotation.AnnotatedBeanDefinition bd) {
                        return bd.getMetadata().isInterface() && bd.getMetadata().isIndependent();
                    }
                };
        scanner.addIncludeFilter(daoQueryFilter());

        for (String pkg : packages) {
            if (pkg == null || pkg.isBlank()) continue;
            LOGGER.debug("scanning package {} for DaoQuery interfaces", pkg);
            scanner.findCandidateComponents(pkg).forEach(bd -> {
                String name = bd.getBeanClassName();
                if (name == null) return;
                try {
                    Class<?> clazz = Class.forName(name);
                    if (clazz != DaoQuery.class && DaoQuery.class.isAssignableFrom(clazz)) {
                        @SuppressWarnings("unchecked")
                        Class<? extends DaoQuery> daoClass = (Class<? extends DaoQuery>) clazz;
                        found.add(daoClass);
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.debug("could not load class {}: {}", name, e.toString());
                }
            });
        }
        return found;
    }

    private static TypeFilter daoQueryFilter() {
        // AssignableTypeFilter loads the class to check assignability. For interface scanning
        // that's acceptable here since we already need Class.forName at registration time.
        return new AssignableTypeFilter(DaoQuery.class) {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory factory) throws IOException {
                // exclude the marker interface itself
                if (DaoQuery.class.getName().equals(metadataReader.getClassMetadata().getClassName())) {
                    return false;
                }
                return super.match(metadataReader, factory);
            }
        };
    }
}

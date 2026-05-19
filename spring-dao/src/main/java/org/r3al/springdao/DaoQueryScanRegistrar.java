package org.r3al.springdao;

import org.r3al.springdao.annotations.DaoQueryScan;
import org.r3al.springdao.impl.DaoQueryRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles {@link DaoQueryScan}. Resolves the explicit basePackages /
 * basePackageClasses; if neither is set, defaults to the package of the annotated
 * configuration class.
 */
public class DaoQueryScanRegistrar implements ImportBeanDefinitionRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryScanRegistrar.class);

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        AnnotationAttributes attrs = AnnotationAttributes.fromMap(
                importingClassMetadata.getAnnotationAttributes(DaoQueryScan.class.getName()));
        if (attrs == null) return;

        List<String> packages = new ArrayList<>();
        for (String p : attrs.getStringArray("basePackages")) {
            if (!p.isBlank()) packages.add(p);
        }
        for (Class<?> c : attrs.getClassArray("basePackageClasses")) {
            packages.add(c.getPackageName());
        }
        if (packages.isEmpty()) {
            packages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }

        LOGGER.debug("@DaoQueryScan registering packages: {}", packages);
        Set<Class<? extends DaoQuery>> daoClasses = new DaoQueryScanner().scan(packages);
        new DaoQueryRegistryImpl(registry).registry(daoClasses);
    }
}

package org.r3al.springdao;

import org.r3al.springdao.impl.DaoQueryRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Registers DaoQuery proxies during configuration parsing. Replaces the legacy
 * BeanFactoryPostProcessor wiring so registration happens in the same classloader
 * as the rest of the configuration (relevant for DevTools restart).
 */
public class DaoQueryAutoRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryAutoRegistrar.class);

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        List<String> packages = new ArrayList<>();
        if (AutoConfigurationPackages.has(beanFactory)) {
            packages.addAll(AutoConfigurationPackages.get(beanFactory));
        }

        String legacyOverride = PropertyUtil.getValue("dao-query.package-scan", null);
        if (legacyOverride != null && !legacyOverride.isBlank()) {
            packages.add(legacyOverride);
        }

        if (packages.isEmpty()) {
            LOGGER.debug("no base packages discovered; use @DaoQueryScan or @SpringBootApplication");
            return;
        }

        LOGGER.debug("scanning packages {} for DaoQuery interfaces", packages);
        Set<Class<? extends DaoQuery>> daoClasses = new DaoQueryScanner().scan(packages);
        LOGGER.debug("{} DaoQuery interfaces discovered", daoClasses.size());
        new DaoQueryRegistryImpl(registry).registry(daoClasses);
    }
}

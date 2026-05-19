package org.r3al.springdao;

import org.r3al.springdao.impl.DaoQueryRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Configuration
public class DaoQueryAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryAutoConfiguration.class);

    public static final String SQL_DIRECTORY = "daoQuery";

    @Bean
    public BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return bf -> {
            LOGGER.debug("starting configuration");
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) bf;

            List<String> packages = new ArrayList<>();
            if (AutoConfigurationPackages.has(bf)) {
                packages.addAll(AutoConfigurationPackages.get(bf));
            }

            // Backward-compatible override: dao-query.package-scan property still adds a package.
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
        };
    }

}

package org.r3al.springdao;

import org.r3al.springdao.impl.DaoQueryRegistryImpl;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class DaoQueryAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryAutoConfiguration.class);

    public static final String SQL_DIRECTORY = "daoQuery";

    @Bean
    public BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return bf -> {
            LOGGER.debug("starting configuration");
            BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) bf;
            String packageScan = PropertyUtil.getValue("dao-query.package-scan", "org.r3al.springdao");
            LOGGER.debug("packageScan {}", packageScan);
            Reflections reflections = new Reflections(packageScan);
            LOGGER.debug("looking for interfaces that implement DaoQuery");
            Set<Class<? extends DaoQuery>> DaoQueryList = reflections.getSubTypesOf(DaoQuery.class);
            LOGGER.debug("{} found interfaces", DaoQueryList.size());
            DaoQueryRegistry DaoQueryRegistry = new DaoQueryRegistryImpl(beanDefinitionRegistry);
            DaoQueryRegistry.registry(DaoQueryList);
        };
    }

}

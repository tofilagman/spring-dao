package org.r3al.springdao.impl;

import org.r3al.springdao.DaoQuery;
import org.r3al.springdao.DaoQueryBeanDefinition;
import org.r3al.springdao.DaoQueryProxyFactory;
import org.r3al.springdao.DaoQueryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.beans.Introspector;
import java.util.Set;

public class DaoQueryRegistryImpl implements DaoQueryRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryRegistryImpl.class);

    private final DaoQueryProxyFactory DaoQueryProxyFactory;

    private final BeanDefinitionRegistry registry;

    public DaoQueryRegistryImpl(BeanDefinitionRegistry registry) {
        this.DaoQueryProxyFactory = new DaoQueryProxyFactoryImpl();
        this.registry = registry;
    }

    @Override
    public void registry(Set<Class<? extends DaoQuery>> DaoQueryList) {
        for (Class<? extends DaoQuery> classe : DaoQueryList) {
            Object source = DaoQueryProxyFactory.create(classe);
            AbstractBeanDefinition beanDefinition = DaoQueryBeanDefinition.of(classe, source);
            String beanName = Introspector.decapitalize(classe.getSimpleName());
            LOGGER.debug("registering the bean {}", beanName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

}

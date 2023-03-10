package org.r3al.springdao.impl

import org.r3al.springdao.DaoQuery
import org.r3al.springdao.DaoQueryBeanDefinition
import org.r3al.springdao.DaoQueryProxyFactory
import org.r3al.springdao.DaoQueryRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import java.beans.Introspector

class DaoQueryRegistryImpl(registry: BeanDefinitionRegistry): DaoQueryRegistry {
    private val daoQueryProxyFactory: DaoQueryProxyFactory
    private val registry: BeanDefinitionRegistry

    init {
        daoQueryProxyFactory = DaoQueryProxyFactoryImpl()
        this.registry = registry
    }

    override fun registry(daoQueryList: Set<Class<out DaoQuery>>) {
        for (classe in daoQueryList) {
            val source = daoQueryProxyFactory.create(classe)
            val beanDefinition = DaoQueryBeanDefinition.of(classe, source)
            val beanName = Introspector.decapitalize(classe.simpleName)
            LOGGER.debug("registering the bean {}", beanName)
            registry.registerBeanDefinition(beanName, beanDefinition)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DaoQueryRegistryImpl::class.java)
    }
}
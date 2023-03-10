package org.r3al.springdao.impl

import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.mockito.Mockito
import org.r3al.springdao.*
import org.r3al.springdao.DaoQueryCache
import org.slf4j.LoggerFactory
import org.springframework.aop.framework.ProxyFactory
import java.util.*

class DaoQueryProxyFactoryImpl: DaoQueryProxyFactory {
    private val daoQueryMethodInterceptor: DaoQueryMethodInterceptor

    private class CacheKey(var className: String, var methodName: String) {
        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val cacheKey = o as CacheKey
            return className == cacheKey.className && methodName == cacheKey.methodName
        }

        override fun hashCode(): Int {
            return Objects.hash(className, methodName)
        }
    }

    init {
        daoQueryMethodInterceptor = DaoQueryMethodInterceptorImpl()
    }

    override fun create(classe: Class<out DaoQuery>): Any {
        LOGGER.debug("creating an {} interface proxy", classe.name)
        val proxy = ProxyFactory()
        proxy.setTarget(Mockito.mock(classe))
        proxy.setInterfaces(classe, DaoQuery::class.java)
        proxy.addAdvice(MethodInterceptor { invocation: MethodInvocation ->
            if ("toString" == invocation.method.name) {
                return@MethodInterceptor "DaoQuery Implementation"
            }
            LOGGER.debug("intercepting the call of method {} of class {}", invocation.method.name, classe.name)
            val info = DaoQueryCache.get(classe, invocation)
            daoQueryMethodInterceptor.executeQuery(info)
        })
        return proxy.getProxy(classe.classLoader)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DaoQueryProxyFactoryImpl::class.java)
        private val cache: Map<CacheKey, DaoQueryInfo> = HashMap()
    }
}
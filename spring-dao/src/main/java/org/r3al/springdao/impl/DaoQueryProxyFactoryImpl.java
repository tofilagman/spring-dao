package org.r3al.springdao.impl;

import org.aopalliance.intercept.MethodInterceptor;
import org.mockito.Mockito;
import org.r3al.springdao.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DaoQueryProxyFactoryImpl implements DaoQueryProxyFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoQueryProxyFactoryImpl.class);

    private final DaoQueryMethodInterceptor DaoQueryMethodInterceptor;

    public DaoQueryProxyFactoryImpl() {
        this.DaoQueryMethodInterceptor = new DaoQueryMethodInterceptorImpl();
    }

    private static class CacheKey {

        String className;

        String methodName;

        public CacheKey(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(className, cacheKey.className) &&
                    Objects.equals(methodName, cacheKey.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, methodName);
        }
    }

    private static final Map<CacheKey, DaoQueryInfo> cache = new HashMap<>();

    @Override
    public Object create(Class<? extends DaoQuery> classe) {
        LOGGER.debug("creating an {} interface proxy", classe.getName());
        ProxyFactory proxy = new ProxyFactory();
        proxy.setTarget(Mockito.mock(classe));
        proxy.setInterfaces(classe, DaoQuery.class);
        proxy.addAdvice((MethodInterceptor) invocation -> {
            if ("toString".equals(invocation.getMethod().getName())) {
                return "DaoQuery Implementation";
            }
            LOGGER.debug("intercepting the call of method {} of class {}", invocation.getMethod().getName(), classe.getName());
            DaoQueryInfo info = DaoQueryCache.get(classe, invocation);
            return DaoQueryMethodInterceptor.executeQuery(info);
        });
        return proxy.getProxy(classe.getClassLoader());
    }

}

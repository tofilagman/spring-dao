package org.r3al.springdao;

public interface DaoQueryProxyFactory {

    Object create(Class<? extends DaoQuery> classe);

}

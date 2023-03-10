package org.r3al.springdao

interface DaoQueryProxyFactory {
    fun create(classe: Class<out DaoQuery>): Any
}
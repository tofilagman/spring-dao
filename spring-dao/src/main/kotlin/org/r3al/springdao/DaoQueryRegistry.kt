package org.r3al.springdao

interface DaoQueryRegistry {
    fun registry(nativeQueryList: Set<Class<out DaoQuery>>)
}
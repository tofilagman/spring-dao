package org.r3al.springdao

interface DaoQueryMethodInterceptor {
    fun executeQuery(info: DaoQueryInfo?): Any?
}
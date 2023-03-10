package org.r3al.springdao

interface DaoQueryConfig {
    val packageScan: String?
    val fileSufix: String?
        get() = "xml"
    val useHibernateTypes: Boolean
        get() = true
    val xmlDirectory: String?
        get() = DaoQueryAutoConfiguration.XML_DIRECTORY
}
package org.r3al.springdao;

public interface DaoQueryConfig {

    String getPackageScan();

    default String getFileSufix() {
        return "sql";
    }

    default boolean getUseHibernateTypes() {
        return true;
    }

    default String getSQLDirectory() {
        return DaoQueryAutoConfiguration.SQL_DIRECTORY;
    }

}

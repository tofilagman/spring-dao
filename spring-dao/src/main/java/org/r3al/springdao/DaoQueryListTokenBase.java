package org.r3al.springdao;

/**
 * Should be inherited when overriding the default implementation class {@link DaoQueryListToken}
 */
public interface DaoQueryListTokenBase {
    Integer getSkip();
    Integer getTake();
}

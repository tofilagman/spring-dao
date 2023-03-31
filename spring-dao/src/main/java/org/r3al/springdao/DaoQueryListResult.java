package org.r3al.springdao;

import java.util.List;

/**
 * This Return Type requires token arguments from {@link DaoQueryListToken}
 *
 * @param <T>
 * @see DaoQueryListToken
 */
public class DaoQueryListResult<T> {

    public DaoQueryListResult() {
    }

    public DaoQueryListResult(List<T> data, Integer count) {
        this.data = data;
        this.count = count;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    private List<T> data;
    private Integer count;
}

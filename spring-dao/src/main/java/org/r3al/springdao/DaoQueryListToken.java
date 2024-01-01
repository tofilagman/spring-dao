package org.r3al.springdao;

public final class DaoQueryListToken implements DaoQueryListTokenBase {

   public DaoQueryListToken(Integer skip, Integer take) {
        this.setSkip(skip);
        this.setTake(take);
    }

    private Integer skip;

    public Integer getSkip() {
        return skip;
    }

    public void setSkip(Integer skip) {
        this.skip = skip;
    }

    public Integer getTake() {
        return take;
    }

    public void setTake(Integer take) {
        this.take = take;
    }

    private Integer take;
}

package model;

import java.math.BigDecimal;
import java.time.Instant;

public class CalculatedRate {
    private String rateName;
    private BigDecimal bid;
    private BigDecimal ask;
    private Instant timestamp;
    public CalculatedRate(String rateName, BigDecimal bid, BigDecimal ask, Instant timestamp) {
        this.rateName = rateName;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }
    public CalculatedRate() {

    }

    public String getRateName() {
        return rateName;
    }

    public void setRateName(String rateName) {
        this.rateName = rateName;
    }

    public BigDecimal getBid() {
        return bid;
    }

    public void setBid(BigDecimal bid) {
        this.bid = bid;
    }

    public BigDecimal getAsk() {
        return ask;
    }

    public void setAsk(BigDecimal ask) {
        this.ask = ask;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    @Override
    public String toString() {
        return rateName + "|" + bid + "|" + ask + "|" + timestamp;// burasu veri formatı
    }
}

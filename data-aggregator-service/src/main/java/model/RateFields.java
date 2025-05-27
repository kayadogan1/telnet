package model;

import java.math.BigDecimal;

public class RateFields {
    private BigDecimal bid;
    private BigDecimal ask;


    // Constructor
    public RateFields(BigDecimal bid, BigDecimal ask, BigDecimal volume) {
        this.bid = bid;
        this.ask = ask;

    }

    // Getters and Setters
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



    @Override
    public String toString() {
        return "Bid: " + bid + ", Ask: " + ask ;
    }
}

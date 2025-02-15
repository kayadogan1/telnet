package com.example.datastorageservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tbl_rates")  // Veritabanı tablosunun adı
public class Rate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Otomatik artan ID
    private Long id;

    @Column(name = "rate_name", nullable = false, length = 10)
    private String rateName;

    @Column(precision = 19, scale = 4)  // decimal(19,4)
    private BigDecimal bid;

    @Column(precision = 19, scale = 4)  // decimal(19,4)
    private BigDecimal ask;

    @Column(name = "rate_updatetime")
    private Instant rateUpdatetime;

    @Column(name = "db_updatetime")
    private Instant dbUpdatetime;

    // Constructor, getter ve setter metotları (veya Lombok kullanabilirsiniz)

    public Rate() {
    }

    public Rate(String rateName, BigDecimal bid, BigDecimal ask, Instant rateUpdatetime) {
        this.rateName = rateName;
        this.bid = bid;
        this.ask = ask;
        this.rateUpdatetime = rateUpdatetime;
        this.dbUpdatetime = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Instant getRateUpdatetime() {
        return rateUpdatetime;
    }

    public void setRateUpdatetime(Instant rateUpdatetime) {
        this.rateUpdatetime = rateUpdatetime;
    }

    public Instant getDbUpdatetime() {
        return dbUpdatetime;
    }

    public void setDbUpdatetime(Instant dbUpdatetime) {
        this.dbUpdatetime = dbUpdatetime;
    }

    @Override
    public String toString() {
        return "Rate{" +
                "id=" + id +
                ", rateName='" + rateName + '\'' +
                ", bid=" + bid +
                ", ask=" + ask +
                ", rateUpdatetime=" + rateUpdatetime +
                ", dbUpdatetime=" + dbUpdatetime +
                '}';
    }
}
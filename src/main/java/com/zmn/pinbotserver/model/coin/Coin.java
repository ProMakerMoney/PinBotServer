package com.zmn.pinbotserver.model.coin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "coins")
public class Coin {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "coin_name", nullable = false)
    private String coinName;

    @Getter
    @Column(name = "timeframe", nullable = false)
    private String timeframe;

    @Setter
    @Getter
    @Column(name = "date_of_addition", nullable = false)
    private long dateOfAddition;

    @Getter
    @Column(name = "min_trading_qty")
    private double minTradingQty;

    @Getter
    @Column(name = "max_trading_qty")
    private double maxTradingQty;

    @Getter
    @Column(name = "min_leverage")
    private int minLeverage;

    @Getter
    @Column(name = "max_leverage")
    private int maxLeverage;

    @Getter
    @Setter
    @Column(name = "data_check", nullable = false)
    private boolean dataCheck;

    @Setter
    @Column(name = "is_counted", nullable = false)
    private boolean isCounted;

    @Setter
    @Getter
    @Column(name = "date_time_counted", nullable = false)
    private long dateTimeCounted;


    public boolean getDataCheck() {
        return dataCheck;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCoinName(String coinName) {
        this.coinName = coinName;
    }

    public void setMinTradingQty(double minTradingQty) {
        this.minTradingQty = minTradingQty;
    }

    public void setMaxTradingQty(double maxTradingQty) {
        this.maxTradingQty = maxTradingQty;
    }

    public void setMinLeverage(int minLeverage) {
        this.minLeverage = minLeverage;
    }

    public void setMaxLeverage(int maxLeverage) {
        this.maxLeverage = maxLeverage;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public boolean getIsCounted() {
        return isCounted;
    }

    public void setIsCounted(boolean counted) {
        isCounted = counted;
    }

    public void setDataCheck(boolean dataCheck) {
        this.dataCheck = dataCheck;
    }
}
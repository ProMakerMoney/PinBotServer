package com.zmn.pinbotserver.model.coin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "coins")
public class Coin {

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    @Column(name = "coin_name", nullable = false)
    private String coinName;

    @Getter
    @Setter
    @Column(name = "timeframe", nullable = false)
    private String timeframe;

    @Getter
    @Setter
    @Column(name = "date_of_addition", nullable = false)
    private long dateOfAddition;

    @Getter
    @Setter
    @Column(name = "min_trading_qty")
    private double minTradingQty;

    @Getter
    @Setter
    @Column(name = "max_trading_qty")
    private double maxTradingQty;

    @Getter
    @Setter
    @Column(name = "min_leverage")
    private int minLeverage;

    @Getter
    @Setter
    @Column(name = "max_leverage")
    private int maxLeverage;


    @Setter
    @Column(name = "data_check", nullable = false)
    private boolean dataCheck;


    @Column(name = "is_counted", nullable = false)
    private boolean isCounted;

    @Getter
    @Setter
    @Column(name = "start_date_time_counted", nullable = false)
    private long startDateTimeCounted;

    @Getter
    @Setter
    @Column(name = "end_date_time_counted", nullable = false)
    private long endDateTimeCounted;

    public void setIsCounted(boolean isCounted) {
        this.isCounted = isCounted;
    }

    public Object getDataCheck() {
        return dataCheck;
    }

    public Object getIsCounted() {
        return isCounted;
    }
}
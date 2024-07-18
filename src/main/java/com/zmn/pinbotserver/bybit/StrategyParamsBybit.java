package com.zmn.pinbotserver.bybit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Параметры стратегии для Bybit.
 */
@Getter
@Setter
public class StrategyParamsBybit implements Serializable {
    private String coinName;
    private String timeFrame;
    private double initialDeposit;
    private double risk;
    private int CCI;
    private int EMA;
    private int leverage;
    private double ratio;
    private int maxOrders;
    private int ATR;
    private double coeff;
    private String status;

    public StrategyParamsBybit(@JsonProperty("coinName") String coinName,
                         @JsonProperty("timeFrame") String timeFrame,
                         @JsonProperty("initialDeposit") double initialDeposit,
                         @JsonProperty("risk") double risk,
                         @JsonProperty("CCI") int CCI,
                         @JsonProperty("EMA") int EMA,
                         @JsonProperty("leverage") int leverage,
                         @JsonProperty("ratio") double ratio,
                         @JsonProperty("maxOrders") int maxOrders,
                         @JsonProperty("ATR") int ATR,
                         @JsonProperty("coeff") double coeff) {
        this.coinName = coinName;
        this.timeFrame = timeFrame;
        this.initialDeposit = initialDeposit;
        this.risk = risk;
        this.CCI = CCI;
        this.EMA = EMA;
        this.leverage = leverage;
        this.ratio = ratio;
        this.maxOrders = maxOrders;
        this.ATR = ATR;
        this.coeff = coeff;
        this.status = "added";
    }
}

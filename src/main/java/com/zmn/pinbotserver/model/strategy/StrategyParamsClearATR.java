package com.zmn.pinbotserver.model.strategy;

import lombok.Getter;

@Getter
public class StrategyParamsClearATR {
    private final String coinName;
    private final String timeframe;
    private final int LEVERAGE;
    private final int ATR_Length;
    private final double coeff;

    public StrategyParamsClearATR(String coinName, String timeframe, int LEVERAGE, int ATR_Length, double coeff) {
        this.coinName = coinName;
        this.timeframe = timeframe;
        this.LEVERAGE = LEVERAGE;
        this.ATR_Length = ATR_Length;
        this.coeff = coeff;
    }
}
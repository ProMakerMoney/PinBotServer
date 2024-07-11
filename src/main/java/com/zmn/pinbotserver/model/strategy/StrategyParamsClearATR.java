package com.zmn.pinbotserver.model.strategy;

import lombok.Getter;

public class StrategyParamsClearATR {
    @Getter
    private final String coinName;
    @Getter
    private final String timeframe;
    @Getter
    private final int LEVERAGE;
    @Getter
    private final int ATR_Length;
    @Getter
    private final double coeff;

    public StrategyParamsClearATR(String coinName, String timeframe, int LEVERAGE, int ATR_Length, double coeff) {
        this.coinName = coinName;
        this.timeframe = timeframe;
        this.LEVERAGE = LEVERAGE;
        this.ATR_Length = ATR_Length;
        this.coeff = coeff;
    }
}
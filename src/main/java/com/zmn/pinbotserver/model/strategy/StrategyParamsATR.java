package com.zmn.pinbotserver.model.strategy;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(force = true)
public class StrategyParamsATR {
    @Getter
    private final String coinName;
    @Getter
    private final String timeframe;
    @Getter
    private final int LEVERAGE;
    @Getter
    private final int MaxOpenOrder;
    @Getter
    private final int CCI;
    @Getter
    private final int EMA;
    @Getter
    private final double RATIO;
    @Getter
    private final int ATR_Length;
    @Getter
    private final double coeff;

    public StrategyParamsATR(String coinName, String timeframe, int LEVERAGE, int maxOpenOrder, int CCI, int EMA, double RATIO, int ATR_Length, double coeff) {
        this.coinName = coinName;
        this.timeframe = timeframe;
        this.LEVERAGE = LEVERAGE;
        this.MaxOpenOrder = maxOpenOrder;
        this.CCI = CCI;
        this.EMA = EMA;
        this.RATIO = RATIO;
        this.ATR_Length = ATR_Length;
        this.coeff = coeff;
    }
}
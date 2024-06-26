package com.zmn.pinbotserver.strategyTesting.model.strategy;

import lombok.Getter;

public class StrategyParams {
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

    public StrategyParams(String coinName, String timeframe, int LEVERAGE, int maxOpenOrder, int CCI, int EMA, double RATIO) {
        this.coinName = coinName;
        this.timeframe = timeframe;
        this.LEVERAGE = LEVERAGE;
        this.MaxOpenOrder = maxOpenOrder;
        this.CCI = CCI;
        this.EMA = EMA;
        this.RATIO = RATIO;
    }
}

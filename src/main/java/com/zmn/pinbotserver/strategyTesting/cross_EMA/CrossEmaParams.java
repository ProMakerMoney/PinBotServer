package com.zmn.pinbotserver.strategyTesting.cross_EMA;

import lombok.Getter;

@Getter
public class CrossEmaParams {
    private final String coinName;
    private final String timeframe;
    private final int LEVERAGE;
    private final int fastEmaLength;
    private final int slowEmaLength;
    private final double takeProfit;
    private final double stopLoss;

    public CrossEmaParams(String coinName, String timeframe, int LEVERAGE, int fastEmaLength, int slowEmaLength, double takeProfit, double stopLoss){
        this.coinName = coinName;
        this.timeframe = timeframe;
        this.LEVERAGE = LEVERAGE;
        this.fastEmaLength = fastEmaLength;
        this.slowEmaLength = slowEmaLength;
        this.takeProfit = takeProfit;
        this.stopLoss = stopLoss;
    }
}
package com.zmn.pinbotserver.bybit;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TradeBot implements Serializable {
    private double initialDeposit;
    private double risk;
    private List<StrategyParamsBybit> strategies;

    public TradeBot(double initialDeposit, double risk) {
        this.initialDeposit = initialDeposit;
        this.risk = risk;
        this.strategies = new ArrayList<>();
    }

    public void addStrategy(StrategyParamsBybit strategy) {
        strategies.add(strategy);
    }
}

@Getter
@Setter
class StrategyParamsBybit implements Serializable {
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

    public StrategyParamsBybit(String coinName, String timeFrame, double initialDeposit, double risk, int CCI, int EMA, int leverage, double ratio, int maxOrders, int ATR, double coeff) {
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

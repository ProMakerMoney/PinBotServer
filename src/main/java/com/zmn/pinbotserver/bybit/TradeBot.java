package com.zmn.pinbotserver.bybit;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    public TradeBot(@JsonProperty("initialDeposit") double initialDeposit, @JsonProperty("risk") double risk) {
        this.initialDeposit = initialDeposit;
        this.risk = risk;
        this.strategies = new ArrayList<>();
    }

    public void addStrategy(StrategyParamsBybit strategy) {
        strategies.add(strategy);
    }
}



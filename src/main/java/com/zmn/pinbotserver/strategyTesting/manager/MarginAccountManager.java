package com.zmn.pinbotserver.strategyTesting.manager;

import com.zmn.pinbotserver.strategyTesting.model.strategy.StrategyParams;

public class MarginAccountManager {

    double coinDEPOSIT;
    double margin;


    double coinCurrentDeposit;
    StrategyParams strategyParams;



    public void updateCurrentDeposit(double profit) {
        coinCurrentDeposit += profit;
    }

    public double calculateQTY(){
        return coinCurrentDeposit / coinDEPOSIT;
    }
}

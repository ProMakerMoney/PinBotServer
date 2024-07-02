package com.zmn.pinbotserver.manager;

import com.zmn.pinbotserver.model.strategy.StrategyParams;

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

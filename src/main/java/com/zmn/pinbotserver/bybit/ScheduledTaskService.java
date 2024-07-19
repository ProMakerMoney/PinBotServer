package com.zmn.pinbotserver.bybit;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ScheduledTaskService {

    private final TradeBotService tradeBotService;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;

    @Autowired
    public ScheduledTaskService(TradeBotService tradeBotService) {
        this.tradeBotService = tradeBotService;
    }

    public void startTask() {
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        if (scheduledFuture == null || scheduledFuture.isCancelled()) {
            scheduledFuture = scheduler.scheduleAtFixedRate(this::updatePriceForAllStrategies, 0, 15, TimeUnit.MINUTES);
            System.out.println("Запланированная задача запущена в " + LocalDateTime.now());
        }
    }

    public void stopTask() {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
            System.out.println("Запланированная задача остановлена в " + LocalDateTime.now());
        }
    }

    private void updatePriceForAllStrategies() {
        List<StrategyATRBybit> strategies = tradeBotService.getStrategies();
        for (StrategyATRBybit strategy : strategies) {
            strategy.onPriceUpdate();
            System.out.println("onPriceUpdate вызван для стратегии: " + strategy.getTradingPair() + " в " + LocalDateTime.now());
        }
    }
}

package com.zmn.pinbotserver.service.tradeBot;

import com.zmn.pinbotserver.bybit.StrategyATRBybit;
import com.zmn.pinbotserver.model.candle.Candle;

import com.zmn.pinbotserver.model.strategy.StrategyParamsATR;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TradeBotService {

    private StrategyATRBybit strategy;
    private StrategyParamsATR currentParams;

    public void startTradeBot(StrategyParamsATR params, double initialDeposit, double risk) {
        // Инициализация стратегии с параметрами
        currentParams = params;
        strategy = new StrategyATRBybit(params, initialDeposit, 0.0, risk); // minTradingQty временно установим в 0.0

        // Запуск шедулера для получения данных каждые 15 минут
        startScheduledTask();
    }

    //@Scheduled(cron = "0 0/15 * * * ?") // Запуск каждую 15-ю минуту часа
    private void startScheduledTask() {
        // Получение данных с биржи
        Candle candle = getCandleFromExchange(currentParams.getCoinName(), currentParams.getTimeframe());

        // Обновление цены в стратегии
        if (strategy != null) {
            strategy.onPriceUpdate(candle);
        }
    }

    private Candle getCandleFromExchange(String tradingPair, String timeframe) {
        // Реализация получения данных с биржи
        // ...
        return new Candle(); // временная заглушка, заменить реальными данными
    }

    public void setTradeMode(String tradingPair, String timeframe, String mode) {
        if (strategy != null && currentParams.getCoinName().equals(tradingPair) && currentParams.getTimeframe().equals(timeframe)) {
            strategy.setMode(mode);
        }
    }
}

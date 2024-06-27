package com.zmn.pinbotserver.strategyTesting.service;

import com.zmn.pinbotserver.historicalData.model.candle.Candle;
import com.zmn.pinbotserver.historicalData.model.coin.Coin;
import com.zmn.pinbotserver.historicalData.service.DataFillerService;
import com.zmn.pinbotserver.strategyTesting.model.order.Order;
import com.zmn.pinbotserver.strategyTesting.model.order.Position;
import com.zmn.pinbotserver.strategyTesting.model.strategy.StrategyParams;
import com.zmn.pinbotserver.strategyTesting.model.strategy.StrategyStats;
import com.zmn.pinbotserver.strategyTesting.strategy.Strategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class StrategyTestingService {

    private final DataFillerService dataFillerService;

    @Autowired
    public StrategyTestingService(DataFillerService dataFillerService) {
        this.dataFillerService = dataFillerService;
    }

    public StrategyStats testStrategy(Coin coin, StrategyParams strategyParams, List<Candle> candles) throws IOException {

        // Создание объекта стратегии с заданными параметрами и начальным депозитом
        Strategy strategy = new Strategy(strategyParams, 100.0, coin.getMinTradingQty(), 10.0);

        // Обработка каждой свечки из подсписка с помощью стратегии
        for (Candle candle : candles) {
            strategy.onPriceUpdate(candle);
        }

        // Получение истории позиций и ордеров из стратегии
        List<Position> positions = strategy.getPositionHistory();
        List<Order> orders = strategy.getOrderHistory();

        // Создание и возвращение объекта StrategyStats
        return new StrategyStats(positions, orders, candles);
    }
}

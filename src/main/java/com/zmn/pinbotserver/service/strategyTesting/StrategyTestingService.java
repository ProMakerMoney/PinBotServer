package com.zmn.pinbotserver.service.strategyTesting;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.model.strategy.StrategyParams;
import com.zmn.pinbotserver.model.strategy.StrategyParamsATR;
import com.zmn.pinbotserver.model.strategy.StrategyStats;
import com.zmn.pinbotserver.service.getData.DataFillerService;
import com.zmn.pinbotserver.strategyTesting.Strategy;
import com.zmn.pinbotserver.strategyTesting.StrategyATR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    public StrategyStats testStrategyATR(Coin coin, StrategyParamsATR strategyParams, List<Candle> candles) throws IOException {

        // Создание объекта стратегии с заданными параметрами и начальным депозитом
        StrategyATR strategy = new StrategyATR(strategyParams, 100.0, coin.getMinTradingQty(), 10.0);

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

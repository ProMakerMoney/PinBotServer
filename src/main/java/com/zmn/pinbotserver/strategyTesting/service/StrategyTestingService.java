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

    public StrategyStats testStrategy(Coin coin, StrategyParams strategyParams) throws IOException {
        // Формирование имени файла на основе торговой пары и таймфрейма
        String fileName = coin.getCoinName() + "_" + coin.getTimeframe() + "_history.csv";

        // Вывод в консоль сообщения о поиске файла
        //System.out.println("Буду искать файл - " + fileName);

        // Указание пути к файлу CSV
        Path filePath = Paths.get("C:\\Users\\PinBot\\IdeaProjects\\PinBotServer\\historical_data", fileName);

        // Чтение свечек из CSV файла с помощью сервиса
        List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);

        // Создание объекта стратегии с заданными параметрами и начальным депозитом
        Strategy strategy = new Strategy(strategyParams, 10.0, coin.getMinTradingQty(), 10.0);

        // Определение количества свечек для обработки
        int candleCount = 2880; // 3 (три) месяца
        // Вычисление начального индекса для подсписка последних 8640 свечек
        int startIndex = Math.max(candles.size() - candleCount, 0);
        // Создание подсписка последних 8640 свечек
        List<Candle> recentCandles = candles.subList(startIndex, candles.size());

        // Обработка каждой свечки из подсписка с помощью стратегии
        for (Candle candle : recentCandles) {
            strategy.onPriceUpdate(candle);
        }

        // Получение истории позиций и ордеров из стратегии
        List<Position> positions = strategy.getPositionHistory();
        List<Order> orders = strategy.getOrderHistory();

        // Создание и возвращение объекта StrategyStats
        return new StrategyStats(positions, orders, recentCandles);
    }
}

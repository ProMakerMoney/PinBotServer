package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.model.strategy.StrategyParams;
import com.zmn.pinbotserver.model.strategy.StrategyStats;
import com.zmn.pinbotserver.repository.CoinRepository;
import com.zmn.pinbotserver.service.DataFillerService;
import com.zmn.pinbotserver.service.StrategyTestingService;
import com.zmn.pinbotserver.strategyTesting.GeneticAlgorithmStrategyTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
public class StrategyCalcController {

    private final CoinRepository coinRepository;
    private final StrategyTestingService strategyTestingService;
    private final DataFillerService dataFillerService;

    @Autowired
    public StrategyCalcController(CoinRepository coinRepository, StrategyTestingService strategyTestingService, DataFillerService dataFillerService) {
        this.coinRepository = coinRepository;
        this.strategyTestingService = strategyTestingService;
        this.dataFillerService = dataFillerService;
    }

    @GetMapping("/api/strategy/calc/{id}")
    public ResponseEntity<String> calculateStrategy(@PathVariable Long id) throws IOException, InterruptedException {
        Optional<Coin> coinOptional = coinRepository.findById(id);

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();
            String fileName = coin.getCoinName() + "_" + coin.getTimeframe() + "_history.csv";
            Path filePath = Paths.get("C:\\Users\\dev-n\\IdeaProjects\\PinBotServer\\historical_data", fileName);
            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);
            // Определение количества свечек для обработки
            int candleCount = 8640; // 3 (три) месяца
            // Вычисление начального индекса для подсписка последних 8640 свечек
            int startIndex = Math.max(candles.size() - candleCount, 0);
            // Создание подсписка последних 8640 свечек
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());
            GeneticAlgorithmStrategyTester tester = new GeneticAlgorithmStrategyTester(recentCandles, strategyTestingService, coin);
            StrategyParams bestParams = tester.run();
            StrategyStats stats = strategyTestingService.testStrategy(coin, bestParams);

            String result = String.format("Лучшие параметры стратегии:\nCCI: %d\nEMA: %d\nLEVERAGE: %d\nRATIO: %.2f\nMAX_ORDERS: %d\n" +
                            "Результаты стратегии:\nОбщая прибыль: %.2f\nКоличество сделок: %d\nПроцент прибыльных сделок: %.2f%%\nМаксимальная просадка: %.2f\nДата тестирования: %d",
                    bestParams.getCCI(), bestParams.getEMA(), bestParams.getLEVERAGE(), bestParams.getRATIO(), bestParams.getMaxOpenOrder(),
                    stats.getProfitInDollars(), stats.getTradeCount(), stats.getProfitableTradePercentage(), stats.getMaxDrawdown(), stats.getTestDate());

            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }
}
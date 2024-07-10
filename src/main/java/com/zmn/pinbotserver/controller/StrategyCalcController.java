package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.model.strategy.StrategyParams;
import com.zmn.pinbotserver.model.strategy.StrategyParamsATR;
import com.zmn.pinbotserver.model.strategy.StrategyStats;
import com.zmn.pinbotserver.storage.CoinRepository;
import com.zmn.pinbotserver.service.getData.DataFillerService;
import com.zmn.pinbotserver.service.strategyTesting.StrategyTestingService;
import com.zmn.pinbotserver.strategyTesting.GenATR;
import com.zmn.pinbotserver.strategyTesting.GeneticAlgorithmStrategyTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
            Path filePath = Paths.get("C:\\Users\\PinBot\\IdeaProjects\\PinBotServer\\historical_data", fileName);
            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);
            // Определение количества свечек для обработки
            int candleCount = 8640; // 3 (три) месяца
            // Вычисление начального индекса для подсписка последних 8640 свечек
            int startIndex = Math.max(candles.size() - candleCount, 0);
            // Создание подсписка последних 8640 свечек
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());

            // Генетический тест
            GeneticAlgorithmStrategyTester tester = new GeneticAlgorithmStrategyTester(recentCandles, strategyTestingService, coin);
            StrategyParams bestGeneticParams = tester.run();

            //Тут надо добавить добавление результатов в БД
            StrategyStats geneticStats = strategyTestingService.testStrategy(coin, bestGeneticParams, recentCandles);

            // Формирование результата
            String result = String.format("Лучшие параметры стратегии (генетический алгоритм):\nCCI: %d\nEMA: %d\nLEVERAGE: %d\nRATIO: %.2f\nMAX_ORDERS: %d\n" +
                            "Результаты стратегии (генетический алгоритм):\nОбщая прибыль: %.2f\nКоличество сделок: %d\nПроцент прибыльных сделок: %.2f%%\nМаксимальная просадка: %.2f\nДата тестирования: %d\n\n",
                    bestGeneticParams.getCCI(), bestGeneticParams.getEMA(), bestGeneticParams.getLEVERAGE(), bestGeneticParams.getRATIO(), bestGeneticParams.getMaxOpenOrder(),
                    geneticStats.getProfitInDollars(), geneticStats.getTradeCount(), geneticStats.getProfitableTradePercentage(), geneticStats.getMaxDrawdown(), geneticStats.getTestDate());

            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }

    @GetMapping("/api/strategy/calcATR/{id}")
    public ResponseEntity<String> calculateStrategyATR(@PathVariable Long id) throws IOException, InterruptedException {
        Optional<Coin> coinOptional = coinRepository.findById(id);

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();
            String fileName = coin.getCoinName() + "_" + coin.getTimeframe() + "_history.csv";
            Path filePath = Paths.get("C:\\Users\\PinBot\\IdeaProjects\\PinBotServer\\historical_data", fileName);
            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);
            // Определение количества свечек для обработки
            int candleCount = 25920; // 3 (три) месяца
            // Вычисление начального индекса для подсписка последних 8640 свечек
            int startIndex = Math.max(candles.size() - candleCount, 0);
            // Создание подсписка последних 8640 свечек
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());

            // Генетический тест
            GenATR tester = new GenATR(recentCandles, strategyTestingService, coin);
            StrategyParamsATR bestGeneticParams = tester.run();

            //Тут надо добавить добавление результатов в БД
            StrategyStats geneticStats = strategyTestingService.testStrategyATR(coin, bestGeneticParams, recentCandles);

            // Формирование результата
            String result = String.format("Лучшие параметры стратегии (генетический алгоритм):\nCCI: %d\nEMA: %d\nLEVERAGE: %d\nRATIO: %.2f\nMAX_ORDERS: %d\nATR_Length: %d\nCoeff: %.2f" +
                            "Результаты стратегии (генетический алгоритм):\nОбщая прибыль: %.2f\nКоличество сделок: %d\nПроцент прибыльных сделок: %.2f%%\nМаксимальная просадка: %.2f\nДата тестирования: %d\n\n",
                    bestGeneticParams.getCCI(), bestGeneticParams.getEMA(), bestGeneticParams.getLEVERAGE(), bestGeneticParams.getRATIO(), bestGeneticParams.getMaxOpenOrder(), bestGeneticParams.getATR_Length(), bestGeneticParams.getCoeff(),
                    geneticStats.getProfitInDollars(), geneticStats.getTradeCount(), geneticStats.getProfitableTradePercentage(), geneticStats.getMaxDrawdown(), geneticStats.getTestDate());

            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }

    @GetMapping("/api/strategy/test/{id}")
    public ResponseEntity<String> testStrategyWithParams(@PathVariable Long id,
                                                         @RequestParam int cci,
                                                         @RequestParam int ema,
                                                         @RequestParam int leverage,
                                                         @RequestParam double ratio,
                                                         @RequestParam int maxOrders) throws IOException {
        Optional<Coin> coinOptional = coinRepository.findById(id);

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();
            String fileName = coin.getCoinName() + "_" + coin.getTimeframe() + "_history.csv";
            Path filePath = Paths.get("C:\\Users\\PinBot\\IdeaProjects\\PinBotServer\\historical_data", fileName);
            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);
            // Определение количества свечек для обработки
            int candleCount = 8640; // 3 (три) месяца
            // Вычисление начального индекса для подсписка последних 2880 свечек
            int startIndex = Math.max(candles.size() - candleCount, 0);
            // Создание подсписка последних 2880 свечек
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());

            // Тестирование стратегии с заданными параметрами
            StrategyParams params = new StrategyParams(coin.getCoinName(), coin.getTimeframe(), leverage, maxOrders, cci, ema, ratio);
            StrategyStats stats = strategyTestingService.testStrategy(coin, params, recentCandles);

            for(Order order : stats.getOrders()){
                System.out.println(order);
            }


            for(Position position : stats.getPositions()){
                System.out.println(position);
            }


            String result = String.format("Результаты стратегии с заданными параметрами:\nCCI: %d\nEMA: %d\nLEVERAGE: %d\nRATIO: %.2f\nMAX_ORDERS: %d\n" +
                            "Общая прибыль: %.2f\nКоличество сделок: %d\nПроцент прибыльных сделок: %.2f%%\nМаксимальная просадка: %.2f\nДата тестирования: %d",
                    cci, ema, leverage, ratio, maxOrders, stats.getProfitInDollars(), stats.getTradeCount(), stats.getProfitableTradePercentage(), stats.getMaxDrawdown(), stats.getTestDate());

            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }
}
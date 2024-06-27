package com.zmn.pinbotserver.strategyTesting.controller;

import com.zmn.pinbotserver.historicalData.model.candle.Candle;
import com.zmn.pinbotserver.historicalData.model.coin.Coin;
import com.zmn.pinbotserver.strategyTesting.model.order.Order;
import com.zmn.pinbotserver.strategyTesting.model.order.Position;
import com.zmn.pinbotserver.strategyTesting.model.strategy.StrategyParams;
import com.zmn.pinbotserver.strategyTesting.model.strategy.StrategyStats;
import com.zmn.pinbotserver.historicalData.repository.CoinRepository;
import com.zmn.pinbotserver.historicalData.service.DataFillerService;
import com.zmn.pinbotserver.strategyTesting.service.StrategyTestingService;
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
            // Вычисление начального индекса для подсписка последних 2880 свечек
            int startIndex = Math.max(candles.size() - candleCount, 0);
            // Создание подсписка последних 2880 свечек
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());

            // Генетический тест
            GeneticAlgorithmStrategyTester tester = new GeneticAlgorithmStrategyTester(recentCandles, strategyTestingService, coin);
            StrategyParams bestGeneticParams = tester.run();

            StrategyStats geneticStats = strategyTestingService.testStrategy(coin, bestGeneticParams, recentCandles);

            // Прямой перебор параметров
            //StrategyParams bestBruteForceParams = performBruteForceTesting(coin, recentCandles);
            //StrategyStats bestBruteForceStats = strategyTestingService.testStrategy(coin, bestBruteForceParams, recentCandles);

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

    private StrategyParams performBruteForceTesting(Coin coin, List<Candle> recentCandles) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Callable<StrategyParams>> tasks = new ArrayList<>();

        for (int cci = 1; cci <= 300; cci += 10) {
            for (int ema = 1; ema <= 300; ema += 10) {
                for (double ratio = 0.5; ratio <= 4.0; ratio += 0.5) {
                    for (int maxOrders = 1; maxOrders <= 10; maxOrders++) {
                        int minLeverage = (int) Math.ceil(coin.getMinTradingQty() * recentCandles.get(0).getClose() / (10.0 / maxOrders));
                        for (int leverage = minLeverage; leverage <= 25; leverage++) {
                            final int fcci = cci;
                            final int fema = ema;
                            final double fratio = ratio;
                            final int fmaxOrders = maxOrders;
                            final int fleverage = leverage;
                            tasks.add(() -> {
                                StrategyParams params = new StrategyParams(coin.getCoinName(), coin.getTimeframe(), fleverage, fmaxOrders, fcci, fema, fratio);
                                StrategyStats stats = strategyTestingService.testStrategy(coin, params, recentCandles);
                                if(stats.getProfitInDollars() > 0 ){
                                    System.out.printf("Текущие параметры: CCI: %d, EMA: %d, LEVERAGE: %d, RATIO: %.2f, MAX_ORDERS: %d%n, Профит: %.2f",
                                            fcci, fema, fleverage, fratio, fmaxOrders,stats.getProfitInDollars());
                                }
                                return params;
                            });
                        }
                    }
                }
            }
        }

        List<Future<StrategyParams>> results = executor.invokeAll(tasks);
        executor.shutdown();

        StrategyParams bestParams = null;
        double bestProfit = Double.NEGATIVE_INFINITY;

        for (Future<StrategyParams> result : results) {
            try {
                StrategyParams params = result.get();
                StrategyStats stats = strategyTestingService.testStrategy(coin, params, recentCandles);

                if (stats.getProfitInDollars() > bestProfit) {
                    bestProfit = stats.getProfitInDollars();
                    bestParams = params;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return bestParams;
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
            int candleCount = 8640; // 2 (три) месяца
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
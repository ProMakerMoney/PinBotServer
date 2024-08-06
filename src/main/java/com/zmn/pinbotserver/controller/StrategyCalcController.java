package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.model.strategy.StrategyParams;
import com.zmn.pinbotserver.model.strategy.StrategyParamsATR;
import com.zmn.pinbotserver.model.strategy.StrategyParamsClearATR;
import com.zmn.pinbotserver.model.strategy.StrategyStats;
import com.zmn.pinbotserver.storage.CoinRepository;
import com.zmn.pinbotserver.service.getData.DataFillerService;
import com.zmn.pinbotserver.service.strategyTesting.StrategyTestingService;
import com.zmn.pinbotserver.strategyTesting.GenATR;
import com.zmn.pinbotserver.strategyTesting.GeneticAlgorithmStrategyTester;
import com.zmn.pinbotserver.strategyTesting.clearATR.GenClearATR;
import com.zmn.pinbotserver.strategyTesting.cross_EMA.CrossEmaParams;
import com.zmn.pinbotserver.strategyTesting.cross_EMA.GenCrossEma;
import com.zmn.pinbotserver.strategyTesting.strategyNEW.Str;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class StrategyCalcController {

    private final CoinRepository coinRepository;
    private final StrategyTestingService strategyTestingService;
    private final DataFillerService dataFillerService;

    private final AtomicBoolean stopCalculation = new AtomicBoolean(false); // Флаг для остановки расчета


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
            Path filePath = Paths.get("historical_data", fileName);
            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);
            // Определение количества свечек для обработки
            int candleCount = 14440; // 3 (три) месяца
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

    @GetMapping("/api/strategy/calcSTR/{id}")
    public ResponseEntity<String> calculateStr(@PathVariable Long id) throws IOException, InterruptedException {
        Optional<Coin> coinOptional = coinRepository.findById(id);

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();
            String fileName = coin.getCoinName() + "_" + coin.getTimeframe() + "_history.csv";
            Path filePath = Paths.get("historical_data", fileName);
            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);
            // Определение количества свечек для обработки
            int candleCount = 960; // 3 (три) месяца
            // Вычисление начального индекса для подсписка последних 8640 свечек
            int startIndex = Math.max(candles.size() - candleCount, 0);
            // Создание подсписка последних 8640 свечек
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());

            Str str = new Str();

            for(Candle c : recentCandles) {
                String signal = str.getSignal(c);
                System.out.println("Свеча: " + c.getTimeAsLocalDateTime() + " Сигнал: " + signal);
            }



            return ResponseEntity.ok("Закончено");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }

    /**
     * Endpoint для расчета стратегии ATR.
     *
     * @param id ID монеты.
     * @return ResponseEntity с результатами расчета.
     * @throws IOException
     * @throws InterruptedException
     */
    @GetMapping("/api/strategy/calcATR/{id}")
    public ResponseEntity<String> calculateStrategyATR(@PathVariable Long id) throws IOException, InterruptedException {
        Optional<Coin> coinOptional = coinRepository.findById(id);

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();
            String fileName = coin.getCoinName() + "_" + coin.getTimeframe() + "_history.csv";
            Path filePath = Paths.get("historical_data", fileName);
            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);
            int candleCount = 2880; // 3 (три) месяца
            int startIndex = Math.max(candles.size() - candleCount, 0);
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());

            GenATR tester = new GenATR(recentCandles, strategyTestingService, coin);
            StrategyParamsATR bestGeneticParams = tester.run();

            StrategyStats geneticStats = strategyTestingService.testStrategyATR(coin, bestGeneticParams, recentCandles);

            // Формируем строку результата в формате CSV
            String result = String.format("%d;%d;%d;%.2f;%d;%d;%.2f;%.2f;%d;%.2f;%.2f;%d",
                    bestGeneticParams.getCCI(), bestGeneticParams.getEMA(), bestGeneticParams.getLEVERAGE(), bestGeneticParams.getRATIO(), bestGeneticParams.getMaxOpenOrder(), bestGeneticParams.getATR_Length(), bestGeneticParams.getCoeff(),
                    geneticStats.getProfitInDollars(), geneticStats.getTradeCount(), geneticStats.getProfitableTradePercentage(), geneticStats.getMaxDrawdown(), geneticStats.getTestDate());

            writeResultsToCsv(coin.getCoinName(), coin.getTimeframe(), result);

            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }

    /**
     * Endpoint для расчета стратегии ATR.
     *
     * @param id ID монеты.
     * @return ResponseEntity с результатами расчета.
     * @throws IOException
     * @throws InterruptedException
     */
    @GetMapping("/api/strategy/calcClearATR/{id}")
    public ResponseEntity<String> calculateStrategyClearATR(@PathVariable Long id) throws IOException, InterruptedException {
        Optional<Coin> coinOptional = coinRepository.findById(id);

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();
            String fileName = coin.getCoinName() + "_" + coin.getTimeframe() + "_history.csv";
            Path filePath = Paths.get("historical_data", fileName);
            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);
            int candleCount = 8640; // 3 (три) месяца
            int startIndex = Math.max(candles.size() - candleCount, 0);
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());

            GenClearATR tester = new GenClearATR(recentCandles, strategyTestingService, coin);
            StrategyParamsClearATR bestGeneticParams = tester.run();

            StrategyStats geneticStats = strategyTestingService.testStrategyClearATR(coin, bestGeneticParams, recentCandles);

            // Формируем строку результата в формате CSV
            String result = String.format("%d;%d;%.2f;%.2f;%d;%.2f;%.2f;%d",
                    bestGeneticParams.getLEVERAGE(), bestGeneticParams.getATR_Length(), bestGeneticParams.getCoeff(),
                    geneticStats.getProfitInDollars(), geneticStats.getTradeCount(), geneticStats.getProfitableTradePercentage(), geneticStats.getMaxDrawdown(), geneticStats.getTestDate());

            writeResultsToCsv(coin.getCoinName(), coin.getTimeframe(), result);

            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }

    /**
     * Endpoint для расчета стратегии ATR.
     *
     * @param id ID монеты.
     * @return ResponseEntity с результатами расчета.
     * @throws IOException
     * @throws InterruptedException
     */
    @GetMapping("/api/strategy/calcEma/{id}")
    public ResponseEntity<String> calculateCrossEMa(@PathVariable Long id) throws IOException, InterruptedException {
        Optional<Coin> coinOptional = coinRepository.findById(id);

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();
            String fileName = coin.getCoinName() + "_" + coin.getTimeframe() + "_history.csv";
            Path filePath = Paths.get("historical_data", fileName);
            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);
            int candleCount = 8640; // 3 (три) месяца
            int startIndex = Math.max(candles.size() - candleCount, 0);
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());

            GenCrossEma tester = new GenCrossEma(recentCandles, strategyTestingService, coin);
            CrossEmaParams bestGeneticParams = tester.run();

            StrategyStats geneticStats = strategyTestingService.testCrossEma(coin, bestGeneticParams, recentCandles);

            // Формируем строку результата в формате CSV
            String result = String.format("%d;%d;%d;%.2f;%.2f;%.2f;%.2f;%.2f",
                    bestGeneticParams.getLEVERAGE(),
                    bestGeneticParams.getFastEmaLength(),
                    bestGeneticParams.getSlowEmaLength(),
                    geneticStats.getProfitInDollars(),
                    geneticStats.getTradeCount(),
                    geneticStats.getProfitableTradePercentage(),
                    geneticStats.getMaxDrawdown(),
                    geneticStats.getTestDate()); // Предполагается, что testDate - это LocalDateTime или аналогичный тип
            writeResultsToCsv(coin.getCoinName(), coin.getTimeframe(), result);

            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }

    /**
     * Метод для расчета стратегий для всех монет
     * @return ResponseEntity с сообщением об успешном выполнении или ошибке
     */
    @GetMapping("/api/strategy/calcAll")
    public ResponseEntity<String> calculateStrategyForAllCoins() throws IOException, InterruptedException {
        stopCalculation.set(false); // Сбрасываем флаг остановки перед началом

        List<Coin> coins = coinRepository.findAll();

        for (Coin coin : coins) {
            if (stopCalculation.get()) {
                break; // Выход из цикла, если установлен флаг остановки
            }

            String fileName = coin.getCoinName() + "_" + coin.getTimeframe() + "_history.csv";
            Path filePath = Paths.get("historical_data", fileName);
            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);
            int candleCount = 8640; // 3 (три) месяца
            int startIndex = Math.max(candles.size() - candleCount, 0);
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());

            GenATR tester = new GenATR(recentCandles, strategyTestingService, coin);
            StrategyParamsATR bestGeneticParams = tester.run();

            StrategyStats geneticStats = strategyTestingService.testStrategyATR(coin, bestGeneticParams, recentCandles);

            // Формируем строку результата в формате CSV
            String result = String.format("%d;%d;%d;%.2f;%d;%d;%.2f;%.2f;%d;%.2f;%.2f;%d",
                    bestGeneticParams.getCCI(), bestGeneticParams.getEMA(), bestGeneticParams.getLEVERAGE(), bestGeneticParams.getRATIO(), bestGeneticParams.getMaxOpenOrder(), bestGeneticParams.getATR_Length(), bestGeneticParams.getCoeff(),
                    geneticStats.getProfitInDollars(), geneticStats.getTradeCount(), geneticStats.getProfitableTradePercentage(), geneticStats.getMaxDrawdown(), geneticStats.getTestDate());

            writeResultsToCsv(coin.getCoinName(), coin.getTimeframe(), result);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при паузе между итерациями: " + e.getMessage());
                }
            }


        return ResponseEntity.ok("Все стратегические расчеты успешно завершены или остановлены.");
    }

    /**
     * Метод для остановки текущего процесса расчета стратегий
     * @return ResponseEntity с сообщением об успешной остановке
     */
    @GetMapping("/api/strategy/stopCalculation")
    public ResponseEntity<String> stopCalculation() {
        stopCalculation.set(true);
        return ResponseEntity.ok("Процесс расчета стратегий остановлен.");
    }

    /**
     * Метод для записи результатов в CSV файл.
     *
     * @param coinName  Имя торговой пары.
     * @param timeframe Таймфрейм.
     * @param result    Результаты для записи.
     */
    private void writeResultsToCsv(String coinName, String timeframe, String result) {
        String fileName = coinName + "_" + timeframe + "_calc.csv";
        Path directoryPath = Paths.get("calcATR");
        Path filePath = directoryPath.resolve(fileName);

        // Создание директории, если она не существует
        try {
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean fileExists = Files.exists(filePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile(), true))) {
            // Записываем шапку, если файл не существует
            if (!fileExists) {
                writer.write("CCI;EMA;LEVERAGE;RATIO;MAX_ORDERS;ATR_Length;Coeff;Profit;Trade Count;Profitable Trade Percentage;Max Drawdown;Test Date");
                writer.newLine();
            }

            // Записываем результаты на новой линии
            writer.write(result);
            writer.newLine();

            System.out.println(String.format("Файл %s создан успешно.", fileName));
        } catch (IOException e) {
            e.printStackTrace();
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
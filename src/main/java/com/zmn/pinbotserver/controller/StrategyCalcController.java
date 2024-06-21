package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.repository.CoinRepository;
import com.zmn.pinbotserver.service.DataFillerService;
import com.zmn.pinbotserver.strategy.Strategy;
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

@RestController // Аннотация, обозначающая, что данный класс является контроллером Spring RESTful веб-сервиса
public class StrategyCalcController {

    // Объявление переменных для репозитория и сервиса
    private final CoinRepository coinRepository;
    DataFillerService dataFillerService;

    // Конструктор с аннотацией @Autowired для автоматической инъекции зависимостей
    @Autowired
    public StrategyCalcController(CoinRepository coinRepository, DataFillerService dataFillerService) {
        this.coinRepository = coinRepository;
        this.dataFillerService = dataFillerService;
    }

    // Метод, обрабатывающий HTTP GET запросы по указанному URL
    @GetMapping("/api/strategy/calc/{id}") // Аннотация, обозначающая, что данный метод обрабатывает HTTP GET запросы по указанному URL
    public ResponseEntity<String> getServerStatus(@PathVariable Long id) throws IOException {
        // Поиск монеты по ID с помощью репозитория
        Optional<Coin> coinOptional = coinRepository.findById(id);

        // Проверка, существует ли монета с указанным ID
        if (coinOptional.isPresent()) {
            // Получение объекта монеты
            Coin coin = coinOptional.get();

            // Формирование имени файла на основе торговой пары и таймфрейма
            String fileName = coin.getCoinName() + "_" + coin.getTimeframe() + "_history.csv";

            // Вывод в консоль сообщения о поиске файла
            System.out.println("Буду искать файл - " + fileName);

            // Указание пути к файлу CSV
            Path filePath = Paths.get("C:\\Users\\dev-n\\IdeaProjects\\PinBotServer\\historical_data", fileName);

            // Чтение свечек из CSV файла с помощью сервиса
            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);

            // Создание объекта стратегии с заданными параметрами
            Strategy strategy = new Strategy(coin.getCoinName(), 10, 93, 27, 0.9805865492105444, 6);

            // Определение количества свечек для обработки
            int candleCount = 8640;
            // Вычисление начального индекса для подсписка последних 8640 свечек
            int startIndex = Math.max(candles.size() - candleCount, 0);
            // Создание подсписка последних 8640 свечек
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());

            // Обработка каждой свечки из подсписка с помощью стратегии
            for (Candle candle : recentCandles) {
                strategy.onPriceUpdate(candle);
            }

            // Получение истории позиций из стратегии
            List<Position> positions = strategy.getPositionHistory();
            // Инициализация переменной для хранения суммарного профита
            double profit = 0.0;
            // Обработка каждой позиции из истории позиций
            for (Position position : positions) {
                // Вывод информации о позиции в консоль
                System.out.println(position);
                // Суммирование прибыли по каждой позиции
                profit += position.getProfit();
            }

            // Возврат HTTP ответа с суммарным профитом
            return ResponseEntity.ok("Суммарный профит = " + profit);
        } else {
            // Возврат HTTP ответа с ошибкой 404, если монета не найдена
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }
}

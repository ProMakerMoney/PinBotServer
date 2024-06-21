package com.zmn.pinbotserver.controller;


import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.model.order.Order;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController // Аннотация, обозначающая, что данный класс является контроллером Spring RESTful веб-сервиса
public class StrategyCalcController {

    private final CoinRepository coinRepository;
    DataFillerService dataFillerService;

    @Autowired
    public StrategyCalcController(CoinRepository coinRepository, DataFillerService dataFillerService) {
        this.coinRepository = coinRepository;
        this.dataFillerService = dataFillerService;
    }

    /**
     * Метод для получения статуса сервера
     * @return int статус сервера: 1, если сервер работает корректно, и 0 в противном случае
     */
    @GetMapping("/api/strategy/calc/{id}") // Аннотация, обозначающая, что данный метод обрабатывает HTTP GET запросы по указанному URL
    public ResponseEntity<String> getServerStatus(@PathVariable Long id) throws IOException {
        Optional<Coin> coinOptional = coinRepository.findById(id);

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();

            // Формирование имени файла на основе торговой пары и таймфрейма
            String fileName = coin.getCoinName() + "_" + coin.getTimeframe() + "_history.csv";

            System.out.println("Буду искать файл - " + fileName);

            // Указание пути к файлу CSV
            Path filePath = Paths.get("C:\\Users\\dev-n\\IdeaProjects\\PinBotServer\\historical_data", fileName);

            List<Candle> candles = dataFillerService.readCandlesFromCsv(filePath);

            Strategy strategy = new Strategy(coin.getCoinName(),10, 93, 27, 0.9805865492105444, 6);

            int candleCount = 8640;
            int startIndex = Math.max(candles.size() - candleCount, 0);
            List<Candle> recentCandles = candles.subList(startIndex, candles.size());

            for (Candle candle : recentCandles) {
                strategy.onPriceUpdate(candle);
            }


            List<Position> positions = strategy.getPositionHistory();
            double profit = 0.0;
            for (Position position : positions) {

                System.out.println(position);
                profit += position.getProfit();
            }

            return ResponseEntity.ok("Суммарный профит = " + profit);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }
}

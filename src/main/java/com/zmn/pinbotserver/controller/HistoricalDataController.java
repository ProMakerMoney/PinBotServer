package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.repository.CoinRepository;
import com.zmn.pinbotserver.service.CoinService;
import com.zmn.pinbotserver.service.HistoricalDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController // Аннотация, обозначающая, что данный класс является контроллером Spring RESTful веб-сервиса
@RequestMapping("/api/data") // Базовый URL для всех методов данного контроллера
public class HistoricalDataController {

    private final CoinService coinService;
    private final HistoricalDataService historicalDataService;
    private final CoinRepository coinRepository;

    @Autowired // Автоматическая инъекция зависимостей от сервисов и репозитория
    public HistoricalDataController(CoinService coinService, HistoricalDataService historicalDataService, CoinRepository coinRepository) {
        this.coinService = coinService;
        this.historicalDataService = historicalDataService;
        this.coinRepository = coinRepository;
    }

    @RestController
    @RequestMapping("/api/data")
    @RequiredArgsConstructor
    public class DataFillerController {

        private final CoinRepository coinRepository;
        private final HistoricalDataService historicalDataService;

        /**
         * Метод для получения исторических данных по монете
         * @param id идентификатор монеты
         * @return ResponseEntity с сообщением об успешном выполнении или ошибке
         */
        @GetMapping("/getHistoricalData/{id}")
        public ResponseEntity<String> getHistoricalData(@PathVariable Long id) {
            Optional<Coin> coinOptional = coinRepository.findById(id);

            if (coinOptional.isPresent()) {
                Coin coin = coinOptional.get();
                List<Candle> candles = historicalDataService.generateHistoricalDataFile(
                        coin.getCoinName(), coin.getTimeframe(),
                        LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.now());

                if (!candles.isEmpty()) {
                    // Обновление дат в таблице coins
                    long startDate = candles.getFirst().getTime();
                    long endDate = candles.getLast().getTime();
                    coin.setStartDateTimeCounted(startDate);
                    coin.setEndDateTimeCounted(endDate);
                    coinRepository.save(coin);
                }

                return ResponseEntity.ok("Данные успешно собраны и сохранены.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
            }
        }
    }
}

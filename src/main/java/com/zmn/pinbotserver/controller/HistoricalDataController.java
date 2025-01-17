package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.storage.CoinRepository;
import com.zmn.pinbotserver.service.coin.CoinService;
import com.zmn.pinbotserver.service.getData.HistoricalDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    /**
     * Метод для получения исторических данных по монете
     * @param id имя монеты
     * @return ResponseEntity с сообщением об успешном выполнении или ошибке
     */
    @GetMapping("/getHistoricalData/{id}")
    public ResponseEntity<String> getHistoricalData(@PathVariable Long id) {
        Optional<Coin> coinOptional = coinRepository.findById(id);

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();

            // Вынесение начальной даты в отдельную переменную
            LocalDateTime startDateTime = LocalDateTime.of(2024, 1, 1, 0, 0);

            List<Candle> candles = historicalDataService.generateHistoricalDataFile(
                    coin.getCoinName(), coin.getTimeframe(),
                    startDateTime, LocalDateTime.now());

            if (!candles.isEmpty()) {
                // Обновление дат в таблице coins
                long startDate = startDateTime.toEpochSecond(ZoneOffset.UTC);
                long endDate = candles.getLast().getTime();
                coin.setStartDateTimeCounted(startDate);
                coin.setEndDateTimeCounted(endDate);
                coin.setDataCheck(true);
                coin.setIsCounted(true);
                coinRepository.updateCoin(coin); // Использование метода update вместо save
            }

            return ResponseEntity.ok("Данные успешно собраны и сохранены.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным именем не найдена.");
        }
    }

    /**
     * Метод для обновления исторических данных всех монет
     * @return ResponseEntity с сообщением об успешном выполнении или ошибке
     */
    @GetMapping("/updateAll")
    public ResponseEntity<String> updateAllHistoricalData() {
        List<Coin> coins = coinRepository.findAll();
        LocalDateTime startDateTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.now();

        for (Coin coin : coins) {
            List<Candle> candles = historicalDataService.generateHistoricalDataFile(
                    coin.getCoinName(), coin.getTimeframe(),
                    startDateTime, endDateTime);

            if (!candles.isEmpty()) {
                long startDate = startDateTime.toEpochSecond(ZoneOffset.UTC);
                long endDate = candles.get(candles.size() - 1).getTime();
                coin.setStartDateTimeCounted(startDate);
                coin.setEndDateTimeCounted(endDate);
                coin.setDataCheck(true);
                coin.setIsCounted(true);
                coinRepository.updateCoin(coin);
            }

            // Пауза в 1 секунду между итерациями
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при паузе между итерациями: " + e.getMessage());
            }
        }

        return ResponseEntity.ok("Все исторические данные успешно обновлены.");
    }
}

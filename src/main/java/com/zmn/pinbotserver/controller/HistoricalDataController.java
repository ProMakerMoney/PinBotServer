package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.repository.CoinRepository;
import com.zmn.pinbotserver.service.CoinService;
import com.zmn.pinbotserver.service.HistoricalDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
     * @param id идентификатор монеты
     * @return ResponseEntity с сообщением об успешном выполнении или ошибке
     */
    @GetMapping("/getHistoricalData/{id}") // Обрабатывает HTTP GET запросы по URL /api/data/getHistoricalData/{id}
    public ResponseEntity<String> getHistoricalData(@PathVariable Long id) {
        Optional<Coin> coinOptional = coinRepository.findById(id); // Поиск монеты по ID

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();
            // Генерация и сохранение файла с историческими данными
            historicalDataService.generateHistoricalDataFile(coin.getCoinName(), coin.getTimeframe(),
                    LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.now());
            return ResponseEntity.ok("Данные успешно собраны и сохранены."); // Возвращаем сообщение об успешном выполнении
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена."); // Возвращаем сообщение об ошибке, если монета не найдена
        }
    }
}

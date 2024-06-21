package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.service.CoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController // Аннотация, обозначающая, что данный класс является контроллером Spring RESTful веб-сервиса
@RequestMapping("/api/coins") // Базовый URL для всех методов данного контроллера
public class CoinController {

    private final CoinService coinService;

    @Autowired // Автоматическая инъекция зависимости от CoinService
    public CoinController(CoinService coinService) {
        this.coinService = coinService;
    }

    /**
     * Метод для добавления новой монеты
     * @param coinName имя монеты
     * @param timeframe таймфрейм
     * @return ResponseEntity с добавленной монетой
     */
    @PostMapping("/add") // Обрабатывает HTTP POST запросы по URL /api/coins/add
    public ResponseEntity<Coin> addCoin(@RequestParam String coinName, @RequestParam String timeframe) {
        if (coinName == null || coinName.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (timeframe == null || timeframe.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        Coin coin = coinService.addCoin(coinName, timeframe);
        return ResponseEntity.ok(coin);
    }


    /**
     * Метод для удаления монеты по ID
     * @param id идентификатор монеты
     * @return ResponseEntity с сообщением об успешном удалении
     */
    @DeleteMapping("/delete/{id}") // Обрабатывает HTTP DELETE запросы по URL /api/coins/delete/{id}
    public ResponseEntity<String> deleteCoin(@PathVariable Long id) {
        coinService.deleteCoin(id);
        return ResponseEntity.ok("Монета удалена успешно.");
    }

    /**
     * Метод для получения всех монет
     * @return ResponseEntity с списком всех монет или сообщением об их отсутствии
     */
    @GetMapping("/all") // Обрабатывает HTTP GET запросы по URL /api/coins/all
    public ResponseEntity<?> getAllCoins() {
        List<Coin> coins = coinService.getAllCoins();
        if (coins.isEmpty()) {
            return ResponseEntity.status(404).body("Монеты не найдены.");
        } else {
            return ResponseEntity.ok(coins);
        }
    }

    /**
     * Метод для получения монеты по ID
     * @param id идентификатор монеты
     * @return ResponseEntity с монетой или сообщением о её отсутствии
     */
    @GetMapping("/{id}") // Обрабатывает HTTP GET запросы по URL /api/coins/{id}
    public ResponseEntity<?> getCoinById(@PathVariable Long id) {
        Optional<Coin> coin = coinService.getCoinById(id);
        if (coin.isPresent()) {
            return ResponseEntity.ok(coin.get());
        } else {
            return ResponseEntity.status(404).body("Монета не найдена.");
        }
    }
}

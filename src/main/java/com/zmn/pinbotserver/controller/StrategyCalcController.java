package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.model.strategy.StrategyParams;
import com.zmn.pinbotserver.repository.CoinRepository;
import com.zmn.pinbotserver.service.StrategyTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;

@RestController // Аннотация, обозначающая, что данный класс является контроллером Spring RESTful веб-сервиса
public class StrategyCalcController {

    // Объявление переменных для репозитория и сервиса
    private final CoinRepository coinRepository;
    private final StrategyTestingService strategyTestingService;

    // Конструктор с аннотацией @Autowired для автоматической инъекции зависимостей
    @Autowired
    public StrategyCalcController(CoinRepository coinRepository, StrategyTestingService strategyTestingService) {
        this.coinRepository = coinRepository;
        this.strategyTestingService = strategyTestingService;
    }

    // Метод, обрабатывающий HTTP GET запросы по указанному URL
    @GetMapping("/api/strategy/calc/{id}") // Аннотация, обозначающая, что данный метод обрабатывает HTTP GET запросы по указанному URL
    public ResponseEntity<String> getServerStatus(@PathVariable Long id) throws IOException {
        // Поиск монеты по ID с помощью репозитория
        Optional<Coin> coinOptional = coinRepository.findById(id);

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();
            StrategyParams strategyParams = new StrategyParams(coin.getCoinName(), coin.getTimeframe(), 10, 10, 93, 27, 0.9805865492105444);
            strategyTestingService.testStrategy(coin, strategyParams);
            return ResponseEntity.ok("Стратегия посчитана!");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }
}

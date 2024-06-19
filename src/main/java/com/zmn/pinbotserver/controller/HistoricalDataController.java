package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.repository.CoinRepository;
import com.zmn.pinbotserver.service.CoinService;
import com.zmn.pinbotserver.service.HistoricalDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/data")
public class HistoricalDataController {

    private final CoinService coinService;
    private final HistoricalDataService historicalDataService;
    private final CoinRepository coinRepository;

    @Autowired
    public HistoricalDataController(CoinService coinService, HistoricalDataService historicalDataService, CoinRepository coinRepository) {
        this.coinService = coinService;
        this.historicalDataService = historicalDataService;
        this.coinRepository = coinRepository;
    }

    @GetMapping("/getHistoricalData/{id}")
    public ResponseEntity<String> getHistoricalData(@PathVariable Long id) {
        Optional<Coin> coinOptional = coinRepository.findById(id);

        if (coinOptional.isPresent()) {
            Coin coin = coinOptional.get();
            historicalDataService.generateHistoricalDataFile(coin.getCoinName(), coin.getTimeframe(),
                    LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.now());
            return ResponseEntity.ok("Данные успешно собраны и сохранены.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Монета с указанным ID не найдена.");
        }
    }

}

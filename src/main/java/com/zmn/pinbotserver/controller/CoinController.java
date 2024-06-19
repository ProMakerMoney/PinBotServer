package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.service.CoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/coins")
public class CoinController {

    private final CoinService coinService;

    @Autowired
    public CoinController(CoinService coinService) {
        this.coinService = coinService;
    }

    @PostMapping("/add")
    public ResponseEntity<Coin> addCoin(@RequestParam String coinName, @RequestParam String timeframe) {
        Coin coin = coinService.addCoin(coinName, timeframe);
        return ResponseEntity.ok(coin);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteCoin(@PathVariable Long id) {
        coinService.deleteCoin(id);
        return ResponseEntity.ok("Монета удалена успешно.");
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllCoins() {
        List<Coin> coins = coinService.getAllCoins();
        if (coins.isEmpty()) {
            return ResponseEntity.status(404).body("Монеты не найдены.");
        } else {
            return ResponseEntity.ok(coins);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCoinById(@PathVariable Long id) {
        Optional<Coin> coin = coinService.getCoinById(id);
        if (coin.isPresent()) {
            return ResponseEntity.ok(coin.get());
        } else {
            return ResponseEntity.status(404).body("Монета не найдена.");
        }
    }
}

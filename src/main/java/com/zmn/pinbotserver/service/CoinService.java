package com.zmn.pinbotserver.service;

import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.repository.CoinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CoinService {

    private final CoinRepository coinRepository;
    private final BybitApiService bybitApiService;

    @Autowired
    public CoinService(CoinRepository coinRepository, BybitApiService bybitApiService) {
        this.coinRepository = coinRepository;
        this.bybitApiService = bybitApiService;
    }

    public Coin addCoin(String coinName, String timeframe) {
        Coin coin = new Coin();


        coin.setCoinName(coinName);
        coin.setTimeframe(timeframe);
        coin.setDateOfAddition(Instant.now().getEpochSecond());
        coin.setMinTradingQty(bybitApiService.getMinTradingQtyFromAPI(coinName));
        coin.setMaxTradingQty(bybitApiService.getMaxTradingQtyFromAPI(coinName));
        coin.setMinLeverage(bybitApiService.getMinLeverageFromAPI(coinName));
        coin.setMaxLeverage(bybitApiService.getMaxLeverageFromAPI(coinName));
        coin.setDataCheck(false);
        coin.setIsCounted(false);
        coin.setDateTimeCounted(0); // Используем 0 как эквивалент 1 января 1970


        return coinRepository.save(coin);
    }

    private double getMinTradingQtyFromAPI(String coinName) {
        return 0.01; // Временно фиксированное значение
    }

    private double getMaxTradingQtyFromAPI(String coinName) {
        return 10.0; // Временно фиксированное значение
    }

    private int getMinLeverageFromAPI(String coinName) {
        return 1; // Временно фиксированное значение
    }

    private int getMaxLeverageFromAPI(String coinName) {
        return 10; // Временно фиксированное значение
    }

    public void deleteCoin(Long id) {
        coinRepository.deleteById(id);
    }

    public List<Coin> getAllCoins() {
        return coinRepository.findAll();
    }

    public Optional<Coin> getCoinById(Long id) {
        return coinRepository.findById(id);
    }
}
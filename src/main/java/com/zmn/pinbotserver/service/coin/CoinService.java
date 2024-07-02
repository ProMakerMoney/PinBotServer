package com.zmn.pinbotserver.service.coin;

import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.service.bybit.BybitApiService;
import com.zmn.pinbotserver.storage.CoinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
        coin.setStartDateTimeCounted(0);
        coin.setEndDateTimeCounted(0);


        return coinRepository.save(coin);
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
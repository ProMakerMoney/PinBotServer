package com.zmn.pinbotserver.historicalData.service;

import com.zmn.pinbotserver.historicalData.model.candle.Candle;
import com.zmn.pinbotserver.historicalData.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandleService {

    private CandleRepository candleStorage;

    public void createTableForCoin(String coin){
        candleStorage.createTableForCoin(coin);
    }

    public void saveCandle(Candle candle) {
        candleStorage.addCandle(candle);
    }

    public void saveAllCandles(List<Candle> candles) {
        candleStorage.addAllCandle(candles);
    }



}
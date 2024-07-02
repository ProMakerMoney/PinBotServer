package com.zmn.pinbotserver.service.candle;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.storage.CandleRepository;
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
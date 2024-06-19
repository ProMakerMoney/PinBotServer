package com.zmn.pinbotserver.repository;

import com.zmn.pinbotserver.model.candle.Candle;

import java.util.Collection;
import java.util.List;


public interface CandleRepository {
    Collection<Candle> findAll();
    void addCandle(Candle candle);
    void addAllCandle(List<Candle> candles);
    void createTableForCoin(String coinName);
}

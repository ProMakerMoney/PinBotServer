package com.zmn.pinbotserver.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmn.pinbotserver.BybitResponse;
import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.strategy.ScalpingStrategyPro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PriceService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TelegramBotService telegramBotService;

    private Map<ScalpingStrategyPro, TradingPairInfo> strategyMap = new HashMap<>();

    public List<Candle> getHistoricalCandles(String tradingPair, String timeframe) {
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - 4320 * 60000; // Начальное время на 3 дня назад от текущего времени
        long endTime = currentTime; // Конечное время - текущее время
        String url = "https://api.bybit.com/derivatives/v3/public/kline?category=linear&symbol=" + tradingPair + "&interval=" + timeframe + "&start=" + startTime + "&end=" + endTime + "&limit=1000";

        try {
            String response = restTemplate.getForObject(url, String.class);
            List<Candle> candles = parseCandles(response);
            candles = candles.reversed();
            candles.removeLast();
            return candles;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void startFetchingData(ScalpingStrategyPro strategy, String tradingPair, String timeframe) {
        strategyMap.put(strategy, new TradingPairInfo(tradingPair, timeframe));
    }

    @Scheduled(cron = "0 * * * * *", zone = "UTC") // Запускать каждую минуту по UTC
    public void fetchCurrentPrice() {


        for (Map.Entry<ScalpingStrategyPro, TradingPairInfo> entry : strategyMap.entrySet()) {
            ScalpingStrategyPro strategy = entry.getKey();
            TradingPairInfo info = entry.getValue();
            fetchAndUpdateStrategy(strategy, info.getTradingPair(), info.getTimeframe());
        }
    }

    private void fetchAndUpdateStrategy(ScalpingStrategyPro strategy, String tradingPair, String timeframe) {

        try {
            // Задержка на 1000 миллисекунд (1 секунд)
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - 150 * 60000; // Начальное время на 15 минут назад от текущего времени
        long endTime = currentTime; // Конечное время - текущее время
        String url = "https://api.bybit.com/derivatives/v3/public/kline?category=linear&symbol=" + tradingPair + "&interval=" + timeframe + "&start=" + startTime + "&end=" + endTime + "&limit=1000";

        try {
            String response = restTemplate.getForObject(url, String.class);
            List<Candle> candles = parseCandles(response);
            candles = candles.reversed();
            candles.removeLast();
            //candles.removeLast();
            if (!candles.isEmpty()) {
                Candle latestCandle = candles.getLast();
                System.out.println("Новая свеча - " + latestCandle.getTime() + " | Цена закрытия: " + latestCandle.getClose());
                strategy.onPriceUpdate(latestCandle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Candle> parseCandles(String json) throws Exception {
        List<Candle> candleList = new ArrayList<>();
        BybitResponse response = objectMapper.readValue(json, BybitResponse.class);
        if (response.getResult() != null && response.getResult().getList() != null) {
            for (List<String> candleData : response.getResult().getList()) {
                long openTime = Long.parseLong(candleData.get(0));
                double open = Double.parseDouble(candleData.get(1));
                double high = Double.parseDouble(candleData.get(2));
                double low = Double.parseDouble(candleData.get(3));
                double close = Double.parseDouble(candleData.get(4));
                double volume = Double.parseDouble(candleData.get(5));

                Candle candle = new Candle(openTime, open, high, low, close, volume);
                candleList.add(candle);
            }
        }
        return candleList;
    }

    private static class TradingPairInfo {
        private String tradingPair;
        private String timeframe;

        public TradingPairInfo(String tradingPair, String timeframe) {
            this.tradingPair = tradingPair;
            this.timeframe = timeframe;
        }

        public String getTradingPair() {
            return tradingPair;
        }

        public String getTimeframe() {
            return timeframe;
        }
    }


}

package com.zmn.pinbotserver.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmn.pinbotserver.BybitResponse;
import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.strategy.Strategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service // Аннотация, обозначающая, что данный класс является сервисом в Spring
public class PriceService {

    @Autowired // Автоматическая инъекция зависимости от RestTemplate
    private RestTemplate restTemplate;

    @Autowired // Автоматическая инъекция зависимости от ObjectMapper
    private ObjectMapper objectMapper;

    private Map<Strategy, TradingPairInfo> strategyMap = new HashMap<>(); // Карта для хранения стратегий и информации о торговых парах

    /**
     * Метод для получения исторических свечей
     * @param tradingPair торговая пара
     * @param timeframe таймфрейм
     * @return список свечей
     */
    public List<Candle> getHistoricalCandles(String tradingPair, String timeframe) {
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - 4320 * 60000; // Начальное время на 3 дня назад от текущего времени
        long endTime = currentTime; // Конечное время - текущее время
        String url = "https://api.bybit.com/derivatives/v3/public/kline?category=linear&symbol=" + tradingPair + "&interval=" + timeframe + "&start=" + startTime + "&end=" + endTime + "&limit=1000";

        try {
            String response = restTemplate.getForObject(url, String.class); // Выполняем запрос и получаем ответ в виде строки
            List<Candle> candles = parseCandles(response); // Парсим ответ и получаем список свечей
            Collections.reverse(candles); // Переворачиваем список свечей
            if (!candles.isEmpty()) {
                candles.remove(candles.size() - 1); // Удаляем последнюю свечу
            }
            return candles;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Метод для запуска получения текущих данных
     * @param strategy стратегия
     * @param tradingPair торговая пара
     * @param timeframe таймфрейм
     */
    public void startFetchingData(Strategy strategy, String tradingPair, String timeframe) {
        strategyMap.put(strategy, new TradingPairInfo(tradingPair, timeframe)); // Добавляем стратегию и информацию о торговой паре в карту
    }

    @Scheduled(cron = "0 * * * * *", zone = "UTC") // Запускать каждую минуту по UTC
    public void fetchCurrentPrice() {
        for (Map.Entry<Strategy, TradingPairInfo> entry : strategyMap.entrySet()) {
            Strategy strategy = entry.getKey();
            TradingPairInfo info = entry.getValue();
            fetchAndUpdateStrategy(strategy, info.getTradingPair(), info.getTimeframe());
        }
    }

    /**
     * Метод для получения и обновления стратегии текущими данными
     * @param strategy стратегия
     * @param tradingPair торговая пара
     * @param timeframe таймфрейм
     */
    private void fetchAndUpdateStrategy(Strategy strategy, String tradingPair, String timeframe) {
        try {
            Thread.sleep(1000); // Задержка на 1000 миллисекунд (1 секунд)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - 150 * 60000; // Начальное время на 15 минут назад от текущего времени
        long endTime = currentTime; // Конечное время - текущее время
        String url = "https://api.bybit.com/derivatives/v3/public/kline?category=linear&symbol=" + tradingPair + "&interval=" + timeframe + "&start=" + startTime + "&end=" + endTime + "&limit=1000";

        try {
            String response = restTemplate.getForObject(url, String.class); // Выполняем запрос и получаем ответ в виде строки
            List<Candle> candles = parseCandles(response); // Парсим ответ и получаем список свечей
            Collections.reverse(candles); // Переворачиваем список свечей
            if (!candles.isEmpty()) {
                candles.remove(candles.size() - 1); // Удаляем последнюю свечу
            }

            if (!candles.isEmpty()) {
                Candle latestCandle = candles.get(candles.size() - 1); // Получаем последнюю свечу
                System.out.println("Новая свеча - " + latestCandle.getTimeAsLocalDateTime() + " | Цена закрытия: " + latestCandle.getClose());
                strategy.onPriceUpdate(latestCandle, 0.1); // Обновляем стратегию новой свечой
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод для парсинга свечей из JSON ответа
     * @param json JSON строка с ответом
     * @return список свечей
     * @throws Exception возможные исключения
     */
    private List<Candle> parseCandles(String json) throws Exception {
        List<Candle> candleList = new ArrayList<>();
        BybitResponse response = objectMapper.readValue(json, BybitResponse.class); // Парсим JSON ответ
        if (response.getResult() != null && response.getResult().getList() != null) {
            for (List<String> candleData : response.getResult().getList()) {
                long openTime = Long.parseLong(candleData.get(0));
                double open = Double.parseDouble(candleData.get(1));
                double high = Double.parseDouble(candleData.get(2));
                double low = Double.parseDouble(candleData.get(3));
                double close = Double.parseDouble(candleData.get(4));
                double volume = Double.parseDouble(candleData.get(5));
                double quoteVolume = Double.parseDouble(candleData.get(6));

                Candle candle = new Candle(openTime, open, high, low, close, volume, quoteVolume); // Создаем объект свечи
                candleList.add(candle); // Добавляем свечу в список
            }
        }
        return candleList;
    }

    /**
     * Вспомогательный класс для хранения информации о торговой паре
     */
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
package com.zmn.pinbotserver.service;


import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.strategy.ScalpingStrategyPro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service // Аннотация, обозначающая, что данный класс является сервисом в Spring
public class StrategyService {

    @Autowired // Автоматическая инъекция зависимости от PriceService
    private PriceService priceService;

    private List<ScalpingStrategyPro> strategies; // Список стратегий

    /**
     * Метод для настройки и запуска стратегии
     */
    public void configureAndStartStrategy() {
        strategies = new ArrayList<>(); // Инициализация списка стратегий

        // Создание новой стратегии с указанными параметрами
        ScalpingStrategyPro strategy = new ScalpingStrategyPro(100, 10, 10, 61, 196, 1.9, "LINKUSDT", 9);

        // Получаем исторические данные для свечей
        List<Candle> historicalCandles = priceService.getHistoricalCandles("LINKUSDT", "15");

        // Вывод всех полученных свечей с биржи
        int num = 0;
        for (Candle candle : historicalCandles) {
            num++;
            System.out.println("Candle: " + num + "  | Дата:" + candle.getTime());
        }

        // Добавляем исторические свечи в стратегию и рассчитываем стратегию на их основе
        for (Candle candle : historicalCandles) {
            strategy.onPriceUpdate(candle);
        }

        // Включение отправки сообщений в стратегии
        strategy.setSendMessages(true);

        // Начинаем получение текущих данных
        priceService.startFetchingData(strategy, "LINKUSDT", "15");
    }
}

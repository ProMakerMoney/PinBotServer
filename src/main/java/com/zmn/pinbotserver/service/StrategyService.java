package com.zmn.pinbotserver.service;


import com.zmn.pinbotserver.InputHandler;
import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.strategy.ScalpingStrategyPro;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StrategyService {

    @Autowired
    private PriceService priceService;

    @Autowired
    private TelegramBotService telegramBotService;

    private List<ScalpingStrategyPro> strategies;


    public void configureAndStartStrategy() {

        telegramBotService.sendMessageToTelegram("Сервер запущен! Ожидаю параметры...");

        strategies = new ArrayList<>();

        ScalpingStrategyPro strategy = new ScalpingStrategyPro(telegramBotService, 100, 10, 10, 61, 196, 1.9, "LINKUSDT", 9);
        // Получаем исторические данные для свечей
        List<Candle> historicalCandles = priceService.getHistoricalCandles("LINKUSDT", "15");

        //Вывод всех полученных свечей с биржи
        int num = 0;
        for (Candle candle : historicalCandles) {
            num++;
            System.out.println("Candle: " + num + "  | Дата:" + candle.getTime());
            //System.out.println("Свеча добавлена - " + candle.getTime());
        }


        // Добавляем исторические свечи в стратегию и рассчитываем стратегию на их основе
        for (Candle candle : historicalCandles) {
            strategy.onPriceUpdate(candle);
            //System.out.println("Свеча добавлена - " + candle.getTime());
        }

        telegramBotService.sendMessageToTelegram("Исторические данные для " + "LINKUSDT" + " получены и загружены!");

        strategy.setSendMessages(true);

        // Начинаем получение текущих данных
        priceService.startFetchingData(strategy, "LINKUSDT", "15");

        // Получение пользовательского ввода для нескольких торговых пар
//        int numberOfPairs = InputHandler.getIntInput("Введите количество торговых пар: ");
//        for (int i = 0; i < numberOfPairs; i++) {
//            String tradingPair = InputHandler.getStringInput("Введите торговую пару " + (i + 1) + ": ") + "USDT";
//            String timeframe = InputHandler.getStringInput("Введите таймфрейм для " + tradingPair + ": ");
//            int cciPeriod = InputHandler.getIntInput("Введите период CCI для " + tradingPair + ": ");
//            int emaPeriod = InputHandler.getIntInput("Введите период EMA для " + tradingPair + ": ");
//            double ratio = InputHandler.getDoubleInput("Введите коэффициент для " + tradingPair + ": ");
//            int leverage = InputHandler.getIntInput("Введите кредитное плечо для " + tradingPair + ": ");
//            int maxOrders = InputHandler.getIntInput("Введите максимальное кол-во сделок для " + tradingPair + ": ");
//
//            // Создаем стратегию с введенными пользователем параметрами
//            ScalpingStrategyPro strategy = new ScalpingStrategyPro(telegramBotService, 100, 10, leverage, cciPeriod, emaPeriod, ratio, tradingPair, maxOrders);
//            strategies.add(strategy);
//
//            telegramBotService.sendMessageToTelegram("Параметры для " + tradingPair + " получены!\nПолучаю исторические данные...");
//
//            // Получаем исторические данные для свечей
//            List<Candle> historicalCandles = priceService.getHistoricalCandles(tradingPair, timeframe);
//
//            //Вывод всех полученных свечей с биржи
//            int num = 0;
//            for (Candle candle : historicalCandles) {
//                num++;
//                System.out.println("Candle: " + num + "  | Дата:" + candle.getTime());
//                //System.out.println("Свеча добавлена - " + candle.getTime());
//            }
//
//
//            // Добавляем исторические свечи в стратегию и рассчитываем стратегию на их основе
//            for (Candle candle : historicalCandles) {
//                strategy.onPriceUpdate(candle);
//                //System.out.println("Свеча добавлена - " + candle.getTime());
//            }
//
//            telegramBotService.sendMessageToTelegram("Исторические данные для " + tradingPair + " получены и загружены!");
//
//            strategy.setSendMessages(true);
//
//            // Начинаем получение текущих данных
//            priceService.startFetchingData(strategy, tradingPair, timeframe);
//        }

        telegramBotService.sendMessageToTelegram("Все стратегии запущены!");
    }
}

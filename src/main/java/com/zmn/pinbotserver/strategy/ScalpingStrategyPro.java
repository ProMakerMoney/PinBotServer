package com.zmn.pinbotserver.strategy;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.OrderManager;
import com.zmn.pinbotserver.model.order.TYPE;
import com.zmn.pinbotserver.service.TelegramBotService;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ScalpingStrategyPro {

    private TelegramBotService telegramBotService;



    double deposit;
    double risk;
    int leverage;
    private final double COMMISSION = 0.1;

    @Getter
    OrderManager orderManager;
    double currentDeposit;
    double currentPrice;
    LocalDateTime currentTime;

    List<Candle> candleHistory = new ArrayList<>();

    int MINIMUM_CANDLES;
    int CCI_PERIOD; // Период для CCI
    final int MAX_CANDLES = 200;

    int EMA_PERIOD;

    double upperBound;
    double lowerBound;

    private List<Double> emaValues = new ArrayList<>();

    private String tradingPair; // Новое поле для хранения информации о торговой паре
    private int maxOrders; // Максимальное количество сделок

    @Setter
    private boolean sendMessages = false; // Флаг для управления отправкой сообщений


    public ScalpingStrategyPro(TelegramBotService telegramBotService, double deposit, double risk, int leverage, int cciPeriod, int emaPeriod, double ratio, String tradingPair, int maxOrders) {
        this.telegramBotService = telegramBotService;
        this.deposit = deposit;
        this.risk = risk;
        this.leverage = leverage;
        this.orderManager = new OrderManager();
        this.currentDeposit = deposit;
        this.CCI_PERIOD = cciPeriod;
        this.MINIMUM_CANDLES = cciPeriod + 1;
        this.EMA_PERIOD = emaPeriod;
        this.upperBound = 100 * ratio;
        this.lowerBound = -100 * ratio;
        this.tradingPair = tradingPair; // Инициализируем новое поле
        this.maxOrders = maxOrders;
    }

    public double calculateEMA(double newValue, int period) {
        if (emaValues.isEmpty()) {
            emaValues.add(newValue);
            return newValue;
        } else {
            double lastEma = emaValues.get(emaValues.size() - 1);
            double alpha = 2.0 / (period + 1);
            double ema = alpha * newValue + (1 - alpha) * lastEma;
            emaValues.add(ema);
            return ema;
        }
    }

    public void onPriceUpdate(Candle candle) {
        if (!candleHistory.contains(candle)) {
            currentPrice = candle.getClose();
            currentTime = candle.getTime();
            candleHistory.add(candle);
            if (sendMessages) {
                System.out.println("Свеча получена!\n\n" + candle + "\n\nАнализирую...");
                telegramBotService.sendMessageToTelegram("Свеча получена!\n\n" + candle + "\n\nАнализирую...");
            }
        } else {
            System.out.println("Свеча в списке уже есть!");
            return;
        }

        if (candleHistory.size() < MINIMUM_CANDLES) {
            System.out.println("Недостаточно данных");
            return;
        }

        if (candleHistory.size() > MAX_CANDLES) {
            candleHistory.removeFirst(); // Удаляем самую старую свечу
        }

        double newCCI = calculateCCI(); // Рассчитываем новое значение CCI
        double newEMA = calculateEMA(newCCI, EMA_PERIOD); // Рассчитываем EMA для нового значения CCI

        if (sendMessages) {
            telegramBotService.sendMessageToTelegram("||| CCI =  " + newCCI + "\n EMA = " + newEMA + " |||");
        }

        manageOrders(newCCI, newEMA); // Обработка логики ордеров

        // Вывод состояния стратегии
        printStrategyState();
    }

    double last_long_price = 0;
    boolean longIsReady = false;
    boolean longIsOpen = false;
    boolean longIsReadyAVG = false;
    boolean cciLongRollback = false;

    double last_short_price = 0;
    boolean shortIsReady = false;
    boolean shortIsOpen = false;
    boolean shortIsReadyAVG = false;
    boolean cciShortRollback = false;

    int openOrders = 0;

    double liquidationLevelPer = (double) 100 / leverage;


    private void manageOrders(double cci, double ema) {
        double profit = 0;

        // Определение уровня ликвидации для LONG и SHORT позиций
        double currentAverageEntryPrice = orderManager.calculateAverageEntryPrice();
        double liquidationPriceLong = currentAverageEntryPrice * (1 - liquidationLevelPer / 100);
        double liquidationPriceShort = currentAverageEntryPrice * (1 + liquidationLevelPer / 100);

        int activeLongOrders = orderManager.countActiveOrdersOfType(TYPE.LONG);
        int activeShortOrders = orderManager.countActiveOrdersOfType(TYPE.SHORT);

        // Ликвидация LONG ордеров, если цена достигает уровня ликвидации
        if (activeLongOrders > 0 && currentPrice <= liquidationPriceLong && activeShortOrders == 0) {
            profit += orderManager.closeAllOrdersOfType(TYPE.LONG, currentPrice, currentTime);
            double netMovement = calculateNetPercentageMovement(currentAverageEntryPrice, currentPrice, TYPE.LONG);
            updateCurrentDeposit(profit);
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Ликвидация всех LONG ордеров. Убыток: " + profit + ". Чистое движение: " + netMovement + "%");
            }
        }

        // Ликвидация SHORT ордеров, если цена достигает уровня ликвидации
        if (activeShortOrders > 0 && currentPrice >= liquidationPriceShort && activeLongOrders == 0) {
            profit += orderManager.closeAllOrdersOfType(TYPE.SHORT, currentPrice, currentTime);
            double netMovement = calculateNetPercentageMovement(currentAverageEntryPrice, currentPrice, TYPE.SHORT);
            updateCurrentDeposit(profit);
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Ликвидация всех SHORT ордеров. Убыток: " + profit + ". Чистое движение: " + netMovement + "%");
            }
        }

        // Проверка условий для открытия первой LONG позиции
        if (cci < lowerBound && !longIsOpen && !longIsReadyAVG && openOrders == 0 && !longIsReady) {
            longIsReady = true; // Устанавливаем флаг готовности для открытия первого LONG
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Приготовились открывать LONG!");
            }
        }

        // Открытие первого LONG ордера при выполнении условий
        if (longIsReady && cci > ema && !longIsReadyAVG && !longIsOpen && cci <= upperBound) {

            orderManager.openOrder(TYPE.LONG, currentPrice, currentTime, calculateMargin(), COMMISSION, leverage);
            last_long_price = currentPrice;
            longIsReady = false;
            longIsOpen = true;
            openOrders++;
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Открытие LONG по цене: " + currentPrice);
            }
        }

        // Установка флага, если CCI возвращается в зону
        if (cci > lowerBound) {
            cciLongRollback = true;
        }

        // Проверка условий для усреднения LONG позиции
        if (longIsOpen && openOrders <= maxOrders && cciLongRollback && cci < lowerBound) {
            longIsReadyAVG = true; // Устанавливаем флаг готовности для усреднения LONG
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Приготовились усреднять LONG!");
            }
        }

        // Открытие усредняющего LONG ордера при выполнении условий
        if (longIsReadyAVG && cci > ema && currentPrice < last_long_price) {

            orderManager.openOrder(TYPE.LONG, currentPrice, currentTime, calculateMargin(), COMMISSION, leverage);
            last_long_price = currentPrice;
            longIsReadyAVG = false;
            cciLongRollback = false;
            openOrders++;
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Усредняем LONG по цене: " + currentPrice + "\nУсреднение " + (openOrders - 1) + " из " + maxOrders);
            }
        }

        // Закрытие всех LONG ордеров при достижении верхней границы CCI
        if (openOrders > 0 && cci > upperBound && activeLongOrders > 0) {


            profit = orderManager.closeAllOrdersOfType(TYPE.LONG, currentPrice, currentTime);
            double netMovement = calculateNetPercentageMovement(currentAverageEntryPrice, currentPrice, TYPE.LONG);
            longIsOpen = false;
            longIsReady = false;
            longIsReadyAVG = false;
            cciLongRollback = false;
            openOrders = 0;
            updateCurrentDeposit(profit);
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Закрытие всех LONG ордеров по цене: " + currentPrice + ". \nПрибыль/убыток: " + profit + ". \nЧистое движение: " + netMovement + "%");
            }
        }

        // Проверка условий для открытия первой SHORT позиции
        if (cci > upperBound && !shortIsOpen && !shortIsReadyAVG && openOrders == 0 && !shortIsReady) {
            shortIsReady = true; // Устанавливаем флаг готовности для открытия первого SHORT
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Приготовились открывать SHORT!");
            }
        }

        // Открытие первого SHORT ордера при выполнении условий
        if (shortIsReady && cci < ema && !shortIsReadyAVG && !shortIsOpen && cci >= lowerBound) {

            orderManager.openOrder(TYPE.SHORT, currentPrice, currentTime, calculateMargin(), COMMISSION, leverage);
            last_short_price = currentPrice;
            shortIsReady = false;
            shortIsOpen = true;
            openOrders++;
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Открытие SHORT по цене: " + currentPrice);
            }
        }

        // Установка флага, если CCI возвращается в зону
        if (cci < upperBound) {
            cciShortRollback = true;
        }

        // Проверка условий для усреднения SHORT позиции
        if (shortIsOpen && openOrders <= maxOrders && cciShortRollback && cci > upperBound) {
            shortIsReadyAVG = true; // Устанавливаем флаг готовности для усреднения SHORT
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Приготовились усреднять SHORT!");
            }
        }

        // Открытие усредняющего SHORT ордера при выполнении условий
        if (shortIsReadyAVG && cci < ema && currentPrice > last_short_price) {

            orderManager.openOrder(TYPE.SHORT, currentPrice, currentTime, calculateMargin(), COMMISSION, leverage);
            last_short_price = currentPrice;
            shortIsReadyAVG = false;
            cciShortRollback = false;
            openOrders++;
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Усредняем SHORT по цене: " + currentPrice + "\nУсреднение " + (openOrders - 1) + " из " + maxOrders);
            }
        }

        // Закрытие всех SHORT ордеров при достижении нижней границы CCI
        if (openOrders > 0 && cci < lowerBound && activeShortOrders > 0) {


            profit = orderManager.closeAllOrdersOfType(TYPE.SHORT, currentPrice, currentTime);
            double netMovement = calculateNetPercentageMovement(currentAverageEntryPrice, currentPrice, TYPE.SHORT);
            shortIsOpen = false;
            shortIsReady = false;
            shortIsReadyAVG = false;
            openOrders = 0;
            updateCurrentDeposit(profit);
            if (sendMessages) {
                telegramBotService.sendMessageToTelegram("Закрытие всех SHORT ордеров по цене: " + currentPrice + ". \nПрибыль/убыток: " + profit + ". \nЧистое движение: " + netMovement + "%");
            }
        }
    }

    // Метод для расчета совокупного чистого движения
    private double calculateNetPercentageMovement(double averageEntryPrice, double exitPrice, TYPE type) {
        double percentageMovement = 0;
        if (type == TYPE.LONG) {
            percentageMovement = ((exitPrice - averageEntryPrice) / averageEntryPrice) * 100;
        } else if (type == TYPE.SHORT) {
            percentageMovement = ((averageEntryPrice - exitPrice) / averageEntryPrice) * 100;
        }
        return percentageMovement;
    }

    public double calculateCCI() {
        int period = CCI_PERIOD; // 20
        if (candleHistory.size() < period) {
            System.out.println("Not enough data to calculate CCI.");
            return 0;
        }

        // Считаем среднее типичных цен за период
        double sumTypicalPrice = 0;
        for (int i = candleHistory.size() - period; i < candleHistory.size(); i++) {
            sumTypicalPrice += candleHistory.get(i).getTypicalPrice();
        }
        double meanTypicalPrice = sumTypicalPrice / period;

        // Считаем среднее абсолютное отклонение от среднего типичной цены
        double sumMeanDeviation = 0;
        for (int i = candleHistory.size() - period; i < candleHistory.size(); i++) {
            sumMeanDeviation += Math.abs(candleHistory.get(i).getTypicalPrice() - meanTypicalPrice);
        }
        double meanDeviation = sumMeanDeviation / period;

        // Рассчитываем CCI
        double cci = (candleHistory.get(candleHistory.size() - 1).getTypicalPrice() - meanTypicalPrice) / (0.015 * meanDeviation);
        return cci;
    }

    public double calculateMargin() {
        double margin = (currentDeposit * risk * leverage) / (100 * maxOrders);
        return Math.max(margin, 0); // Гарантируем, что маржа не может быть меньше нуля
    }

    public void updateCurrentDeposit(double profit) {
        currentDeposit += profit;
    }

    // Новый метод для вывода состояния стратегии
    private void printStrategyState() {
        StringBuilder state = new StringBuilder();
        state.append("Стратегия для торговой пары ").append(tradingPair).append(": \n");

        if (longIsOpen) {
            state.append("Открыта позиция: LONG\n");
        } else if (shortIsOpen) {
            state.append("Открыта позиция: SHORT\n");
        } else {
            state.append("Позиция не открыта\n");
        }

        if (longIsReady) {
            state.append("Готова открыть позицию: LONG\n");
        } else if (shortIsReady) {
            state.append("Готова открыть позицию: SHORT\n");
        } else {
            state.append("Не готова открыть новую позицию\n");
        }

        if (longIsReadyAVG) {
            state.append("Готова усреднять позицию: LONG\n");
        } else if (shortIsReadyAVG) {
            state.append("Готова усреднять позицию: SHORT\n");
        } else {
            state.append("Не готова усреднять позицию\n");
        }

        System.out.println(state.toString());
        if (sendMessages) {
            telegramBotService.sendMessageToTelegram(state.toString());
        }
    }
}

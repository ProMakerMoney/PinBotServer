package com.zmn.pinbotserver.strategy;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.model.order.STATUS;
import com.zmn.pinbotserver.model.order.TYPE;
import com.zmn.pinbotserver.model.strategy.StrategyParams;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;


public class Strategy {

    // Параметры стратегии
    int LEVERAGE; // Плечо для торговли
    int CCI_PERIOD; // Период для расчета CCI
    int EMA_PERIOD; // Период для расчета EMA
    private final int MAXOrders; // Максимальное количество одновременно открытых сделок

    double upperBound; // Верхняя граница для CCI
    double lowerBound; // Нижняя граница для CCI

    double currentPrice; // Текущая цена инструмента

    List<Candle> candleHistory = new ArrayList<>(); // История свечей

    int MINIMUM_CANDLES = 300; // Минимальное количество свечей для расчетов

    private final List<Double> emaValues = new ArrayList<>(); // Список значений EMA

    String tradingPair;

    @Getter
    List<Order> orderHistory = new ArrayList<>();

    @Getter
    List<Position> positionHistory = new ArrayList<>();

    // Конструктор стратегии
    public Strategy(StrategyParams strategyParams) {
        this.tradingPair = strategyParams.getCoinName();
        this.LEVERAGE = strategyParams.getLEVERAGE();
        this.CCI_PERIOD = strategyParams.getCCI();
        this.EMA_PERIOD = strategyParams.getEMA();
        this.upperBound = 100 * strategyParams.getRATIO();
        this.lowerBound = -100 * strategyParams.getRATIO();
        this.MAXOrders = strategyParams.getMaxOpenOrder();
    }

    /**
     * Метод для расчета EMA
     * @param newValue новое значение для включения в расчет
     * @param period период EMA
     * @return рассчитанное значение EMA
     */
    public double calculateEMA(double newValue, int period) {
        if (emaValues.isEmpty()) {
            // Если это первое значение, просто добавляем его и возвращаем
            emaValues.add(newValue);
            return newValue;
        } else {
            // Расчет EMA на основе предыдущего значения
            double lastEma = emaValues.getLast();
            double alpha = 2.0 / (period + 1);
            double ema = alpha * newValue + (1 - alpha) * lastEma;
            emaValues.add(ema);
            return ema;
        }
    }

    /**
     * Метод для расчета CCI (Commodity Channel Index)
     * @return рассчитанное значение CCI
     */
    public double calculateCCI() {
        int period = CCI_PERIOD; // Период для CCI
        if (candleHistory.size() < period) {
            // Если недостаточно данных, возвращаем 0 и выводим сообщение
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

        // Рассчитываем CCI по формуле
        return (candleHistory.getLast().getTypicalPrice() - meanTypicalPrice) / (0.015 * meanDeviation);
    }

    /**
     * Метод, вызываемый при обновлении цены
     * @param candle новая свеча
     */
    public void onPriceUpdate(Candle candle) {
        // Если свеча уже существует в истории, возвращаемся
        if (!candleHistory.contains(candle)) {
            currentPrice = candle.getClose();
            candleHistory.add(candle);
        } else {
            System.out.println("Свеча в списке уже есть!");
            return;
        }

        // Если недостаточно данных, возвращаемся
        if (candleHistory.size() < MINIMUM_CANDLES) {
            System.out.println("Недостаточно данных");
            return;
        }

        // Если данных слишком много, удаляем старейшие данные
        if (candleHistory.size() > MINIMUM_CANDLES) {
            candleHistory.removeFirst(); // Удаляем самую старую свечу
        }

        // Рассчитываем новые значения CCI и EMA
        double newCCI = calculateCCI();
        double newEMA = calculateEMA(newCCI, EMA_PERIOD);

        // Обрабатываем ордера на основе новых значений
        manageOrders(newCCI, newEMA, candle);

        // Вывод состояния стратегии
        // printStrategyState();
    }

    // Переменные состояния для управления позициями
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

    Position position;

    /**
     * Метод для управления ордерами
     * @param cci текущее значение CCI
     * @param ema текущее значение EMA
     */
    private void manageOrders(double cci, double ema, Candle candle) {

        double liquidationLevelPer = 100.0 / LEVERAGE; // Уровень ликвидации в процентах

        // Ликвидация LONG ордеров, если цена достигает уровня ликвидации
        if (longIsOpen && currentPrice <= last_long_price * (1 - liquidationLevelPer / 100)) {
            System.out.println("Ликвидация LONG позиции");
            System.out.printf("Пара: %s, Действие: Продажа, Время свечи: %s, Количество: %.2f, Текущая цена: %.2f, Статус: %s%n", tradingPair, candle.getTime(), 0.1 * openOrders, currentPrice, STATUS.CLOSE);
            //orderHistory.add(new Order(tradingPair, "Sell", candle.getTime(), 0.1 * openOrders, currentPrice, STATUS.CLOSE));
            longIsOpen = false;
            longIsReady = false;
            longIsReadyAVG = false;
            cciLongRollback = false;
            openOrders = 0;
        }

        // Ликвидация SHORT ордеров, если цена достигает уровня ликвидации
        if (shortIsOpen && currentPrice >= last_short_price * (1 + liquidationLevelPer / 100)) {
            System.out.println("Ликвидация SHORT позиции");
            System.out.printf("Пара: %s, Действие: Покупка, Время свечи: %s, Количество: %.2f, Текущая цена: %.2f, Статус: %s%n", tradingPair, candle.getTime(), 0.1 * openOrders, currentPrice, STATUS.CLOSE);
            //orderHistory.add(new Order(tradingPair, "Buy", candle.getTime(), 0.1 * openOrders, currentPrice, STATUS.CLOSE));
            shortIsOpen = false;
            shortIsReady = false;
            shortIsReadyAVG = false;
            cciShortRollback = false;
            openOrders = 0;
        }

        // Проверка условий для открытия первой LONG позиции
        if (cci < lowerBound && !longIsOpen && !longIsReadyAVG && openOrders == 0 && !longIsReady) {
            longIsReady = true; // Устанавливаем флаг готовности для открытия первого LONG
        }

        // Открытие первого LONG ордера при выполнении условий
        if (longIsReady && cci > ema && !longIsReadyAVG && !longIsOpen && cci <= upperBound && !shortIsOpen && !shortIsReadyAVG) {
            System.out.println("Открыть LONG");
            openOrders++;
            System.out.printf("Пара: %s, Действие: Покупка, Время свечи: %s, Количество: %.2f, Текущая цена: %.2f, Статус: %s%n", tradingPair, candle.getTime(), 0.1 * openOrders, currentPrice, STATUS.OPEN);
            position = new Position(tradingPair, TYPE.LONG, LEVERAGE);
            Order order = new Order(tradingPair, "buy", candle.getTime(), 0.1, currentPrice, STATUS.OPEN);
            position.addOrder(order);
            orderHistory.add(order);
            last_long_price = currentPrice;
            longIsReady = false;
            longIsOpen = true;

        }

        // Установка флага, если CCI возвращается в зону
        if (cci > lowerBound) {
            cciLongRollback = true;
        }

        // Проверка условий для усреднения LONG позиции
        if (longIsOpen && openOrders <= MAXOrders && cciLongRollback && cci < lowerBound) {
            longIsReadyAVG = true; // Устанавливаем флаг готовности для усреднения LONG
        }

        // Открытие усредняющего LONG ордера при выполнении условий
        if (longIsReadyAVG && cci > ema && currentPrice < last_long_price && longIsOpen && !shortIsOpen && !shortIsReadyAVG) {
            System.out.println("Усреднить LONG");
            openOrders++;
            System.out.printf("Пара: %s, Действие: Покупка, Время свечи: %s, Количество: %.2f, Текущая цена: %.2f, Статус: %s%n", tradingPair, candle.getTime(), 0.1 * openOrders, currentPrice, STATUS.OPEN);
            Order order = new Order(tradingPair, "buy", candle.getTime(), 0.1, currentPrice, STATUS.OPEN);
            position.addOrder(order);
            orderHistory.add(order);
            last_long_price = currentPrice;
            longIsReadyAVG = false;
            cciLongRollback = false;

        }

        // Закрытие всех LONG ордеров при достижении верхней границы CCI
        if (longIsOpen && !shortIsOpen && openOrders > 0 && cci > upperBound) {
            System.out.println("Закрыть все LONG позиции");
            System.out.printf("Пара: %s, Действие: Продажа, Время свечи: %s, Количество: %.2f, Текущая цена: %.2f, Статус: %s%n", tradingPair, candle.getTime(), 0.1 * openOrders, currentPrice, STATUS.CLOSE);
            Order order = new Order(tradingPair, "sell", candle.getTime(), 0.1 * openOrders, currentPrice, STATUS.CLOSE);
            position.closePosition(order);
            orderHistory.add(order);
            longIsOpen = false;
            longIsReady = false;
            longIsReadyAVG = false;
            cciLongRollback = false;
            openOrders = 0;

            positionHistory.add(position);
        }

        // Проверка условий для открытия первой SHORT позиции
        if (cci > upperBound && !shortIsOpen && !shortIsReadyAVG && openOrders == 0 && !shortIsReady) {
            shortIsReady = true; // Устанавливаем флаг готовности для открытия первого SHORT
        }

        // Открытие первого SHORT ордера при выполнении условий
        if (shortIsReady && cci < ema && !shortIsReadyAVG && !shortIsOpen && cci >= lowerBound && !longIsOpen && !longIsReadyAVG) {
            System.out.println("Открыть SHORT позицию");
            openOrders++;
            System.out.printf("Пара: %s, Действие: Продажа, Время свечи: %s, Количество: %.2f, Текущая цена: %.2f, Статус: %s%n", tradingPair, candle.getTime(), 0.1, currentPrice, STATUS.OPEN);
            position = new Position(tradingPair, TYPE.SHORT, LEVERAGE);
            Order order = new Order(tradingPair, "sell", candle.getTime(), 0.1, currentPrice, STATUS.OPEN);
            position.addOrder(order);
            orderHistory.add(order);
            last_short_price = currentPrice;
            shortIsReady = false;
            shortIsOpen = true;
        }

        // Установка флага, если CCI возвращается в зону
        if (cci < upperBound) {
            cciShortRollback = true;
        }

        // Проверка условий для усреднения SHORT позиции
        if (shortIsOpen && openOrders <= MAXOrders && cciShortRollback && cci > upperBound) {
            shortIsReadyAVG = true; // Устанавливаем флаг готовности для усреднения SHORT
        }

        // Открытие усредняющего SHORT ордера при выполнении условий
        if (shortIsReadyAVG && cci < ema && currentPrice > last_short_price && !longIsOpen && shortIsOpen) {
            System.out.println("Усреднить SHORT");
            openOrders++;
            System.out.printf("Пара: %s, Действие: Продажа, Время свечи: %s, Количество: %.2f, Текущая цена: %.2f, Статус: %s%n", tradingPair, candle.getTime(), 0.1, currentPrice, STATUS.OPEN);
            Order order = new Order(tradingPair, "sell", candle.getTime(), 0.1, currentPrice, STATUS.OPEN);
            position.addOrder(order);
            orderHistory.add(order);
            last_short_price = currentPrice;
            shortIsReadyAVG = false;
            cciShortRollback = false;

        }

        // Закрытие всех SHORT ордеров при достижении нижней границы CCI
        if (!longIsOpen && shortIsOpen && openOrders > 0 && cci < lowerBound) {
            System.out.println("Закрыть все SHORT позиции");
            System.out.printf("Пара: %s, Действие: Покупка, Время свечи: %s, Количество: %.2f, Текущая цена: %.2f, Статус: %s%n", tradingPair, candle.getTime(), 0.1 * openOrders, currentPrice, STATUS.CLOSE);
            Order order = new Order(tradingPair, "buy", candle.getTime(), 0.1 * openOrders, currentPrice, STATUS.CLOSE);
            position.closePosition(order);
            orderHistory.add(order);

            //orderHistory.add(new Order(tradingPair, "Buy", candle.getTime(), 0.1 * openOrders, currentPrice, STATUS.CLOSE));
            shortIsOpen = false;
            shortIsReady = false;
            shortIsReadyAVG = false;
            openOrders = 0;

            positionHistory.add(position);
        }
    }

    /**
     * Метод для вывода состояния стратегии
     */
    private void printStrategyState() {
        StringBuilder state = new StringBuilder();



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

        System.out.println(state);
    }
}

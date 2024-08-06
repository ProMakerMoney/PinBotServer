package com.zmn.pinbotserver.strategyTesting;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.model.order.STATUS;
import com.zmn.pinbotserver.model.order.TYPE;
import com.zmn.pinbotserver.model.strategy.StrategyParams;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


public class Strategy {

    boolean strategyAllowed = true; // Флаг, разрешающий или запрещающий работу стратегии

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

    double initialDeposit;
    double risk;

    double currentDeposit;

    double marginPerOrder;
    double marginQTY;
    double minTradingQty;



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

    @Setter
    @Getter
    String mode = "free"; // режим по умолчанию


    // Конструктор стратегии
    public Strategy(StrategyParams strategyParams, double initialDeposit, double minTradingQty, double risk) {
        this.tradingPair = strategyParams.getCoinName();
        this.LEVERAGE = strategyParams.getLEVERAGE();
        this.CCI_PERIOD = strategyParams.getCCI();
        this.EMA_PERIOD = strategyParams.getEMA();
        this.upperBound = 100 * strategyParams.getRATIO();
        this.lowerBound = -100 * strategyParams.getRATIO();
        this.MAXOrders = strategyParams.getMaxOpenOrder();
        this.initialDeposit = initialDeposit;
        this.currentDeposit = initialDeposit;
        this.minTradingQty = minTradingQty;
        this.risk = risk;
        calculateInitialMarginPerOrder();
    }

    /**
     * Метод для расчета маржи на сделку.
     */
    private void calculateInitialMarginPerOrder() {
        this.marginQTY = (currentDeposit * (risk / 100) * LEVERAGE) / (MAXOrders * currentPrice);
        this.marginQTY = Math.floor(this.marginQTY * 10) / 10.0;

        if (this.marginQTY < this.minTradingQty) {
            if (this.marginQTY >= this.minTradingQty * 0.7) {
                this.marginQTY = this.minTradingQty;
                strategyAllowed = true;
            } else {
                strategyAllowed = false;
            }
        }
    }


    /**
     * Метод для расчета Average True Range (ATR).
     *
     * @param candleHistory Список свечей.
     * @param ATR_length Период для ATR.
     * @return Значение ATR.
     * @throws IllegalArgumentException если недостаточно данных для расчета ATR.
     */
    public double calculateATR(List<Candle> candleHistory, int ATR_length) {
        if (candleHistory.size() < ATR_length) {
            throw new IllegalArgumentException("Недостаточно данных для расчета ATR.");
        }
        double atr = 0.0;
        for (int i = candleHistory.size() - ATR_length; i < candleHistory.size(); i++) {
            Candle currentCandle = candleHistory.get(i);
            Candle previousCandle = candleHistory.get(i - 1);
            double highLow = currentCandle.getHigh() - currentCandle.getLow();
            double highClose = Math.abs(currentCandle.getHigh() - previousCandle.getClose());
            double lowClose = Math.abs(currentCandle.getLow() - previousCandle.getClose());
            double trueRange = Math.max(highLow, Math.max(highClose, lowClose));
            atr += trueRange;
        }
        return atr / ATR_length;
    }

    /**
     * Метод для расчета EMA.
     *
     * @param newValue Новое значение для включения в расчет.
     * @param period Период EMA.
     * @return Рассчитанное значение EMA.
     */
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

    /**
     * Метод для расчета CCI (Commodity Channel Index).
     *
     * @return Рассчитанное значение CCI.
     */
    public double calculateCCI() {
        int period = CCI_PERIOD;
        if (candleHistory.size() < period) {
            return 0;
        }
        double sumTypicalPrice = 0;
        for (int i = candleHistory.size() - period; i < candleHistory.size(); i++) {
            sumTypicalPrice += candleHistory.get(i).getTypicalPrice();
        }
        double meanTypicalPrice = sumTypicalPrice / period;
        double sumMeanDeviation = 0;
        for (int i = candleHistory.size() - period; i < candleHistory.size(); i++) {
            sumMeanDeviation += Math.abs(candleHistory.get(i).getTypicalPrice() - meanTypicalPrice);
        }
        double meanDeviation = sumMeanDeviation / period;
        return (candleHistory.get(candleHistory.size() - 1).getTypicalPrice() - meanTypicalPrice) / (0.015 * meanDeviation);
    }

    /**
     * Метод, вызываемый при обновлении цены.
     *
     * @param candle Новая свеча.
     */
    public void onPriceUpdate(Candle candle) {
        if (!candleHistory.contains(candle)) {
            currentPrice = candle.getClose();
            candleHistory.add(candle);
        } else {
            return;
        }
        if (candleHistory.size() < MINIMUM_CANDLES) {
            return;
        }
        if (candleHistory.size() > MINIMUM_CANDLES) {
            candleHistory.remove(0);
        }

        double newCCI = calculateCCI();
        double newEMA = calculateEMA(newCCI, EMA_PERIOD);
        checkFirstLongReady(newCCI);
        checkFirstShortReady(newCCI);
        checkLongAverageReady(newCCI);
        checkShortAverageReady(newCCI);
        if (canOpenFirstLongPosition(newCCI, newEMA)) {
            openLongPosition(candle);
        }
        if (canOpenFirstShortPosition(newCCI, newEMA)) {
            openShortPosition(candle);
        }
        if (canAverageLongPosition(newCCI, newEMA)) {
            averageLongPosition(candle);
        }
        if (canAverageShortPosition(newCCI, newEMA)) {
            averageShortPosition(candle);
        }
        if (canCloseLongPosition(newCCI)) {
            closeLongPosition(candle);
        }
        if (canCloseShortPosition(newCCI)) {
            closeShortPosition(candle);
        }
    }

    /**
     * Метод для проверки готовности открытия первой LONG позиции.
     *
     * @param cci Текущее значение CCI.
     */
    private void checkFirstLongReady(double cci) {
        if (cci < lowerBound && !longIsOpen && !longIsReadyAVG && openOrders == 0 && !longIsReady) {
            longIsReady = true;
        }
    }

    /**
     * Метод для проверки готовности открытия первой SHORT позиции.
     *
     * @param cci Текущее значение CCI.
     */
    private void checkFirstShortReady(double cci) {
        if (cci > upperBound && !shortIsOpen && !shortIsReadyAVG && openOrders == 0 && !shortIsReady) {
            shortIsReady = true;
        }
    }

    /**
     * Метод для проверки готовности усреднения LONG позиции.
     *
     * @param cci Текущее значение CCI.
     */
    private void checkLongAverageReady(double cci) {
        if (longIsOpen && openOrders <= MAXOrders && cci > lowerBound) {
            cciLongRollback = true;
        }
        if (longIsOpen && openOrders <= MAXOrders && cciLongRollback && cci < lowerBound) {
            longIsReadyAVG = true;
        }
    }

    /**
     * Метод для проверки готовности усреднения SHORT позиции.
     *
     * @param cci Текущее значение CCI.
     */
    private void checkShortAverageReady(double cci) {
        if (shortIsOpen && openOrders <= MAXOrders && cci < upperBound) {
            cciShortRollback = true;
        }
        if (shortIsOpen && openOrders <= MAXOrders && cciShortRollback && cci > upperBound) {
            shortIsReadyAVG = true;
        }
    }

    /**
     * Метод для проверки условий открытия первой LONG позиции.
     *
     * @param cci Текущее значение CCI.
     * @param ema Текущее значение EMA.
     * @return true, если условия выполнены, иначе false.
     */
    private boolean canOpenFirstLongPosition(double cci, double ema) {
        return longIsReady && cci > ema && !longIsReadyAVG && !longIsOpen && cci <= upperBound && !shortIsOpen && !shortIsReadyAVG  && openOrders < MAXOrders;
    }

    /**
     * Метод для проверки условий открытия первой SHORT позиции.
     *
     * @param cci Текущее значение CCI.
     * @param ema Текущее значение EMA.
     * @return true, если условия выполнены, иначе false.
     */
    private boolean canOpenFirstShortPosition(double cci, double ema) {
        return shortIsReady && cci < ema && !shortIsReadyAVG && !shortIsOpen && cci >= lowerBound && !longIsOpen && !longIsReadyAVG  && openOrders < MAXOrders;
    }

    /**
     * Метод для проверки условий усреднения LONG позиции.
     *
     * @param cci Текущее значение CCI.
     * @param ema Текущее значение EMA.
     * @return true, если условия выполнены, иначе false.
     */
    private boolean canAverageLongPosition(double cci, double ema) {
        return longIsReadyAVG && cci > ema && currentPrice < last_long_price && longIsOpen && !shortIsOpen && !shortIsReadyAVG  && openOrders < MAXOrders;
    }

    /**
     * Метод для проверки условий усреднения SHORT позиции.
     *
     * @param cci Текущее значение CCI.
     * @param ema Текущее значение EMA.
     * @return true, если условия выполнены, иначе false.
     */
    private boolean canAverageShortPosition(double cci, double ema) {
        return shortIsReadyAVG && cci < ema && currentPrice > last_short_price && !longIsOpen && shortIsOpen  && openOrders < MAXOrders;
    }

    /**
     * Метод для проверки условий закрытия LONG позиции.
     *
     * @param cci Текущее значение CCI.
     * @return true, если условия выполнены, иначе false.
     */
    private boolean canCloseLongPosition(double cci) {
        double liquidationLevelPer = 100.0 / LEVERAGE; // Уровень ликвидации в процентах
        return (longIsOpen && (currentPrice <= position.getAverageEnterPrice() * (1 - liquidationLevelPer / 100)) ||
                (cci > upperBound && longIsOpen && openOrders > 0));
    }

    /**
     * Метод для проверки условий закрытия SHORT позиции.
     *
     * @param cci Текущее значение CCI.
     * @return true, если условия выполнены, иначе false.
     */
    private boolean canCloseShortPosition(double cci) {
        double liquidationLevelPer = 100.0 / LEVERAGE; // Уровень ликвидации в процентах
        return (shortIsOpen && (currentPrice >= position.getAverageEnterPrice() * (1 + liquidationLevelPer / 100)) ||
                (cci < lowerBound && shortIsOpen && openOrders > 0));
    }

    /**
     * Метод для открытия LONG позиции.
     *
     * @param candle Свеча.
     */
    private void openLongPosition(Candle candle) {
        calculateInitialMarginPerOrder();
        if (currentDeposit < marginPerOrder * MAXOrders) {
            return;
        }
        openOrders++;
        position = new Position(tradingPair, TYPE.LONG, LEVERAGE);
        Order order = new Order(tradingPair, "buy", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        last_long_price = currentPrice;
        longIsReady = false;
        longIsOpen = true;
    }

    /**
     * Метод для усреднения LONG позиции.
     *
     * @param candle Свеча.
     */
    private void averageLongPosition(Candle candle) {
        if (currentDeposit < marginPerOrder) {
            return;
        }
        openOrders++;
        Order order = new Order(tradingPair, "buy", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        last_long_price = currentPrice;
        longIsReadyAVG = false;
        cciLongRollback = false;
    }

    /**
     * Метод для закрытия LONG позиции.
     *
     * @param candle Свеча.
     */
    private void closeLongPosition(Candle candle) {
        Order order = new Order(tradingPair, "sell", candle.getTime(), marginQTY * openOrders, currentPrice, STATUS.CLOSE);
        position.closePosition(order);
        orderHistory.add(order);
        double profit = position.getProfit();
        currentDeposit += profit;
        longIsOpen = false;
        longIsReady = false;
        longIsReadyAVG = false;
        cciLongRollback = false;
        openOrders = 0;
        positionHistory.add(position);
    }

    /**
     * Метод для открытия SHORT позиции.
     *
     * @param candle Свеча.
     */
    private void openShortPosition(Candle candle) {
        calculateInitialMarginPerOrder();
        if (currentDeposit < marginPerOrder * MAXOrders) {
            return;
        }
        openOrders++;
        position = new Position(tradingPair, TYPE.SHORT, LEVERAGE);
        Order order = new Order(tradingPair, "sell", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        last_short_price = currentPrice;
        shortIsReady = false;
        shortIsOpen = true;
    }

    /**
     * Метод для усреднения SHORT позиции.
     *
     * @param candle Свеча.
     */
    private void averageShortPosition(Candle candle) {
        if (currentDeposit < marginPerOrder) {
            return;
        }
        openOrders++;
        Order order = new Order(tradingPair, "sell", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        last_short_price = currentPrice;
        shortIsReadyAVG = false;
        cciShortRollback = false;
    }

    /**
     * Метод для закрытия SHORT позиции.
     *
     * @param candle Свеча.
     */
    private void closeShortPosition(Candle candle) {
        Order order = new Order(tradingPair, "buy", candle.getTime(), marginQTY * openOrders, currentPrice, STATUS.CLOSE);
        position.closePosition(order);
        orderHistory.add(order);
        double profit = position.getProfit();
        currentDeposit += profit;
        shortIsOpen = false;
        shortIsReady = false;
        shortIsReadyAVG = false;
        openOrders = 0;
        positionHistory.add(position);
    }
}
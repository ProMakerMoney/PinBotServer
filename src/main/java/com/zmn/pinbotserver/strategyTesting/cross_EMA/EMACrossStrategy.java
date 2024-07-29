package com.zmn.pinbotserver.strategyTesting.cross_EMA;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.model.order.STATUS;
import com.zmn.pinbotserver.model.order.TYPE;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

// Класс стратегии
public class EMACrossStrategy  {

    boolean strategyAllowed = true; // Флаг, разрешающий или запрещающий работу стратегии

    // Параметры стратегии
    int LEVERAGE; // Плечо для торговли


    double currentPrice; // Текущая цена инструмента

    List<Candle> candleHistory = new ArrayList<>(); // История свечей

    int MINIMUM_CANDLES = 300; // Минимальное количество свечей для расчетов

    String tradingPair;

    @Getter
    List<Order> orderHistory = new ArrayList<>();

    @Getter
    List<Position> positionHistory = new ArrayList<>();

    double risk;

    double currentDeposit;

    double marginPerOrder;
    double marginQTY;
    double minTradingQty;

    int fastEMAPeriod;
    int slowEMAPeriod;


    boolean longIsOpen = false;
    boolean shortIsOpen = false;

    int openOrders = 0;

    Position position;

    // Переменные для хранения истории значений EMA
    private final List<Double> fastEMAs = new ArrayList<>();
    private final List<Double> slowEMAs = new ArrayList<>();

    private Double fastEMA;
    private Double slowEMA;

    private double takeProfit;
    private double stopLoss;


    /**
     * Конструктор стратегии Cross EMA.
     *
     * @param strategyParams Параметры стратегии.
     * @param initialDeposit Начальный депозит.
     * @param minTradingQty Минимальное количество для торговли.
     * @param risk Уровень риска.
     */
    public EMACrossStrategy (CrossEmaParams strategyParams, double initialDeposit, double minTradingQty, double risk) {
        this.tradingPair = strategyParams.getCoinName();
        this.LEVERAGE = strategyParams.getLEVERAGE();
        this.currentDeposit = initialDeposit;
        this.minTradingQty = minTradingQty;
        this.risk = risk;
        this.fastEMAPeriod = strategyParams.getFastEmaLength();
        this.slowEMAPeriod= strategyParams.getSlowEmaLength();
        this.takeProfit = strategyParams.getTakeProfit();
        this.stopLoss = strategyParams.getStopLoss();
        calculateInitialMarginPerOrder();
    }

    /**
     * Метод для расчета маржи на сделку.
     */
    private void calculateInitialMarginPerOrder() {
        if (currentPrice <= 0) {
            strategyAllowed = false;
            return;
        }

        this.marginQTY = (currentDeposit * (risk / 100) * LEVERAGE) / currentPrice;
        this.marginQTY = Math.floor(this.marginQTY * 10) / 10.0;

        if (this.marginQTY < this.minTradingQty) {
            if (this.marginQTY >= this.minTradingQty * 0.9) {
                this.marginQTY = this.minTradingQty;
                strategyAllowed = true;
            } else {
                strategyAllowed = false;
            }
        } else {
            strategyAllowed = true;
        }
    }

    // Метод для расчета и обновления быстрой EMA
    private void updateFastEMA(double newPrice) {
        if (fastEMAs.isEmpty()) {
            fastEMA = newPrice; // Инициализация при первой цене
        } else {
            double multiplier = 2.0 / (fastEMAPeriod + 1);
            fastEMA = (newPrice - fastEMA) * multiplier + fastEMA;
        }
        fastEMAs.add(fastEMA); // Хранение значения EMA в списке
    }

    // Метод для расчета и обновления медленной EMA
    private void updateSlowEMA(double newPrice) {
        if (slowEMAs.isEmpty()) {
            slowEMA = newPrice; // Инициализация при первой цене
        } else {
            double multiplier = 2.0 / (slowEMAPeriod + 1);
            slowEMA = (newPrice - slowEMA) * multiplier + slowEMA;
        }
        slowEMAs.add(slowEMA); // Хранение значения EMA в списке
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

        // Проверка минимального количества свечей для расчетов
        if (candleHistory.size() < MINIMUM_CANDLES) {
            return;
        }

        // Удаление старых данных, чтобы поддерживать размер истории свечей
        if (candleHistory.size() > MINIMUM_CANDLES) {
            candleHistory.removeFirst(); // Используем remove(0) вместо removeFirst()
        }

        updateFastEMA(candle.getClose()); // Обновление быстрой EMA
        updateFastEMA(candle.getClose()); // Обновление медленной EMA

        double takeProfitPer = takeProfit / LEVERAGE;
        double stopLossPer = stopLoss / LEVERAGE;

        if(!longIsOpen && !shortIsOpen){
            if (fastEMA > slowEMA) {
                openLongPosition(candle);
            } else if (fastEMA < slowEMA) {
                openShortPosition(candle);
            }
        }

        if(longIsOpen && !shortIsOpen){
            if (fastEMA < slowEMA
                    || (currentPrice >= position.getAverageEnterPrice() * (1 + takeProfitPer / 100))
                    || (currentPrice <= position.getAverageEnterPrice() * (1 - stopLossPer / 100))){
                closeLongPosition(candle);
            }
            if (fastEMA < slowEMA){
                openShortPosition(candle);
            }
        }

        if(!longIsOpen && shortIsOpen){
            if(fastEMA > slowEMA
                    || (currentPrice >= position.getAverageEnterPrice() * (1 - takeProfit / 100))
                    || (currentPrice <= position.getAverageEnterPrice() * (1 + stopLoss / 100))) {
                closeShortPosition(candle);
            }
            if (fastEMA > slowEMA){
                openLongPosition(candle);
            }
        }
    }


    /**
     * Метод для открытия LONG позиции.
     *
     * @param candle Свеча.
     */
    private void openLongPosition(Candle candle) {
        calculateInitialMarginPerOrder();
        if (currentDeposit < marginPerOrder) {
            return;
        }
        openOrders++;
        position = new Position(tradingPair, TYPE.LONG, LEVERAGE);
        Order order = new Order(tradingPair, "buy", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        longIsOpen = true;
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
        if (currentDeposit < marginPerOrder) {
            return;
        }
        openOrders++;
        position = new Position(tradingPair, TYPE.SHORT, LEVERAGE);
        Order order = new Order(tradingPair, "sell", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        shortIsOpen = true;
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
        openOrders = 0;
        positionHistory.add(position);
    }
}
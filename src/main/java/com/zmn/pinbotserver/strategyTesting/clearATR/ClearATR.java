package com.zmn.pinbotserver.strategyTesting.clearATR;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.model.order.STATUS;
import com.zmn.pinbotserver.model.order.TYPE;
import com.zmn.pinbotserver.model.strategy.StrategyParamsClearATR;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class ClearATR {

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

    double initialDeposit;
    double risk;

    double currentDeposit;

    double marginPerOrder;
    double marginQTY;
    double minTradingQty;

    int ATR_length;
    double coeff; // множитель

    // Поля для AlphaTrend
    double alphaTrend = 0.0;
    double prevAlphaTrend = 0.0;
    boolean alphaTrendBuy = false;
    boolean alphaTrendSell = false;

    TYPE direction = null;


    boolean longIsOpen = false;



    boolean shortIsOpen = false;


    int openOrders = 0;

    Position position;

    @Setter
    @Getter
    String mode = "free"; // режим по умолчанию

    /**
     * Конструктор стратегии ATR.
     *
     * @param strategyParams Параметры стратегии.
     * @param initialDeposit Начальный депозит.
     * @param minTradingQty Минимальное количество для торговли.
     * @param risk Уровень риска.
     */
    public ClearATR(StrategyParamsClearATR strategyParams, double initialDeposit, double minTradingQty, double risk) {
        this.tradingPair = strategyParams.getCoinName();
        this.LEVERAGE = strategyParams.getLEVERAGE();
        this.initialDeposit = initialDeposit;
        this.currentDeposit = initialDeposit;
        this.minTradingQty = minTradingQty;
        this.risk = risk;
        this.ATR_length = strategyParams.getATR_Length();
        this.coeff = strategyParams.getCoeff();
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

    /**
     * Метод для расчета AlphaTrend индикатора.
     *
     * @param atr Значение ATR.
     * @param coeff Множитель для AlphaTrend.
     * @param period Период для AlphaTrend.
     */
    public void calcAlphaTrend(Candle candle, double atr, double coeff, int period) {
        double low = candle.getLow(); // Текущая минимальная цена
        double high = candle.getHigh(); // Текущая максимальная цена

        double upT = low - atr * coeff;
        double downT = high + atr * coeff;
        prevAlphaTrend = alphaTrend;
        double mfi = taMFI(candleHistory, period);

        alphaTrend = (mfi >= 50) ? Math.max(upT, prevAlphaTrend) : Math.min(downT, prevAlphaTrend);
        alphaTrendBuy = taCrossover(alphaTrend, prevAlphaTrend);
        alphaTrendSell = taCrossunder(alphaTrend, prevAlphaTrend);

        if (alphaTrendBuy && (direction == TYPE.SHORT || direction == null)) {
            direction = TYPE.LONG;
            if (shortIsOpen) {
                closeShortPosition(candle);
                shortIsOpen = false;
            }
            if (!longIsOpen) {
                openLongPosition(candle); // Открытие позиции
                longIsOpen = true;
            }
        } else if (alphaTrendSell && (direction == TYPE.LONG || direction == null)) {
            direction = TYPE.SHORT;
            if (longIsOpen) {
                closeLongPosition(candle);
                longIsOpen = false;
            }
            if (!shortIsOpen) {
                openShortPosition(candle); // Открытие позиции
                shortIsOpen = true;
            }
        }
    }

    /**
     * Метод для определения пересечения снизу.
     *
     * @param current Текущее значение.
     * @param previous Предыдущее значение.
     * @return true, если произошло пересечение снизу, иначе false.
     */
    public boolean taCrossover(double current, double previous) {
        return current > previous;
    }

    /**
     * Метод для определения пересечения сверху.
     *
     * @param current Текущее значение.
     * @param previous Предыдущее значение.
     * @return true, если произошло пересечение сверху, иначе false.
     */
    public boolean taCrossunder(double current, double previous) {
        return current < previous;
    }

    /**
     * Метод для расчета среднего значения High, Low и Close (HLC3).
     *
     * @param candle Объект свечи.
     * @return Среднее значение HLC3.
     */
    public double hlc3(Candle candle) {
        return (candle.getHigh() + candle.getLow() + candle.getClose()) / 3;
    }

    /**
     * Метод для расчета Money Flow Index (MFI).
     *
     * @param candleHistory Список свечей.
     * @param period Период для MFI.
     * @return Значение MFI.
     * @throws IllegalArgumentException если недостаточно данных для расчета MFI.
     */
    public double taMFI(List<Candle> candleHistory, int period) {
        if (candleHistory.size() < period + 1) {
            throw new IllegalArgumentException("Недостаточно данных для расчета MFI.");
        }
        double positiveMoneyFlow = 0.0;
        double negativeMoneyFlow = 0.0;

        for (int i = candleHistory.size() - period; i < candleHistory.size(); i++) {
            Candle currentCandle = candleHistory.get(i);
            Candle previousCandle = candleHistory.get(i - 1);

            double currentTypicalPrice = hlc3(currentCandle);
            double previousTypicalPrice = hlc3(previousCandle);

            double moneyFlow = currentTypicalPrice * currentCandle.getVolume();
            if (currentTypicalPrice > previousTypicalPrice) {
                positiveMoneyFlow += moneyFlow;
            } else if (currentTypicalPrice < previousTypicalPrice) {
                negativeMoneyFlow += moneyFlow;
            }
        }

        // Prevent division by zero
        if (negativeMoneyFlow == 0) {
            return 100;
        }

        double moneyFlowRatio = positiveMoneyFlow / negativeMoneyFlow;
        return 100 - (100 / (1 + moneyFlowRatio));
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

        // Вычисление True Range для каждого периода
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
            candleHistory.remove(0); // Используем remove(0) вместо removeFirst()
        }

        double atr = calculateATR(candleHistory, ATR_length);
        calcAlphaTrend(candle, atr, coeff, ATR_length);
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



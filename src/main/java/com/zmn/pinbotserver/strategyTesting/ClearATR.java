package com.zmn.pinbotserver.strategyTesting;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.model.order.STATUS;
import com.zmn.pinbotserver.model.order.TYPE;
import com.zmn.pinbotserver.model.strategy.StrategyParamsClearATR;
import lombok.Getter;

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

    TYPE direction;

    // Конструктор стратегии
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
     * Метод для расчета маржи на сделку и соответствующего количества монет
     */
    private void calculateInitialMarginPerOrder() {
        this.marginQTY = (currentDeposit * (risk / 100) * LEVERAGE) / (currentPrice);

        // Округляем marginQTY в меньшую сторону до одного знака после запятой
        this.marginQTY = Math.floor(this.marginQTY * 10) / 10.0;

        // Проверяем, если marginQTY меньше minTradingQty не более чем на 10%
        if (this.marginQTY < this.minTradingQty) {
            if (this.marginQTY >= this.minTradingQty * 0.9) {
                this.marginQTY = this.minTradingQty;
                strategyAllowed = true; // Разрешаем стратегии работать
            } else {
                strategyAllowed = false; // Запрещаем стратегии работать
            }
        }
    }

    /**
     * Метод для расчета AlphaTrend индикатора.
     *
     * @param closePrice текущая цена закрытия
     * @param low        текущая минимальная цена
     * @param high       текущая максимальная цена
     * @param atr        значение ATR
     * @param coeff      множитель для AlphaTrend
     * @param period     период для AlphaTrend
     * @return сигнал AlphaTrend ("BUY", "SELL" или "HOLD")
     */
    public TYPE calcAlphaTrend(double closePrice, double low, double high, double atr, double coeff, int period) {
        double upT = low - atr * coeff;
        double downT = high + atr * coeff;

        prevAlphaTrend = alphaTrend;

        double mfi = taMFI(candleHistory, period);
        alphaTrend = (mfi >= 50) ? (upT < prevAlphaTrend ? prevAlphaTrend : upT) : (downT > prevAlphaTrend ? prevAlphaTrend : downT);

        alphaTrendBuy = taCrossover(alphaTrend, prevAlphaTrend);
        alphaTrendSell = taCrossunder(alphaTrend, prevAlphaTrend);

        if (alphaTrendBuy) {
            return TYPE.LONG;
        } else if (alphaTrendSell) {
            return TYPE.SHORT;
        } else {
            return TYPE.HOLD;
        }
    }

    /**
     * Метод для определения пересечения снизу.
     *
     * @param current  текущее значение
     * @param previous предыдущее значение
     * @return true, если произошло пересечение снизу, иначе false
     */
    public boolean taCrossover(double current, double previous) {
        return current > previous;
    }

    /**
     * Метод для определения пересечения сверху.
     *
     * @param current  текущее значение
     * @param previous предыдущее значение
     * @return true, если произошло пересечение сверху, иначе false
     */
    public boolean taCrossunder(double current, double previous) {
        return current < previous;
    }

    /**
     * Вспомогательный метод для расчета среднего значения High, Low и Close (HLC3).
     *
     * @param candle объект свечи
     * @return среднее значение HLC3
     */
    public double hlc3(Candle candle) {
        return (candle.getHigh() + candle.getLow() + candle.getClose()) / 3;
    }

    /**
     * Метод для расчета Money Flow Index (MFI).
     *
     * @param candleHistory список свечей
     * @param period        период для MFI
     * @return значение MFI
     * @throws IllegalArgumentException если недостаточно данных для расчета MFI
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

        double moneyFlowRatio = positiveMoneyFlow / negativeMoneyFlow;
        double mfi = 100 - (100 / (1 + moneyFlowRatio));

        return mfi;
    }

    /**
     * Метод для расчета Average True Range (ATR).
     *
     * @param candleHistory список свечей
     * @param ATR_length    период для ATR
     * @return значение ATR
     * @throws IllegalArgumentException если недостаточно данных для расчета ATR
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
     * Метод, вызываемый при обновлении цены
     * @param candle новая свеча
     */
    public void onPriceUpdate(Candle candle) {
        // Если свеча уже существует в истории, возвращаемся
        if (!candleHistory.contains(candle)) {
            currentPrice = candle.getClose();
            candleHistory.add(candle);
        } else {
            return;
        }

        // Если недостаточно данных, возвращаемся
        if (candleHistory.size() < MINIMUM_CANDLES) {
            return;
        }

        // Если данных слишком много, удаляем старейшие данные
        if (candleHistory.size() > MINIMUM_CANDLES) {
            candleHistory.remove(0); // Удаляем самую старую свечу
        }

        double atr = calculateATR(candleHistory, ATR_length);
        TYPE type = calcAlphaTrend(candle.getClose(), candle.getLow(), candle.getHigh(), atr, coeff, ATR_length);

        // Обрабатываем ордера на основе новых значений
        if(strategyAllowed) {
            manageOrders(candle, type);
        }
    }

    // Переменные состояния для управления позициями
    double last_long_price = 0;

    boolean longIsOpen = false;

    double last_short_price = 0;

    boolean shortIsOpen = false;

    int openOrders = 0;

    Position position;

    /**
     * Метод для управления ордерами
     */
    private void manageOrders(Candle candle, TYPE type) {
        double liquidationLevelPer = 100.0 / LEVERAGE; // Уровень ликвидации в процентах

        // Ликвидация LONG ордеров, если цена достигает уровня ликвидации
        if (longIsOpen && currentPrice <= last_long_price * (1 - liquidationLevelPer / 100)) {
            closeLongPosition(candle);
        }

        // Ликвидация SHORT ордеров, если цена достигает уровня ликвидации
        if (shortIsOpen && currentPrice >= last_short_price * (1 + liquidationLevelPer / 100)) {
            closeShortPosition(candle);
        }


        // Открытие первого LONG ордера при выполнении условий
        if (type == TYPE.LONG && !longIsOpen && !shortIsOpen) {
            openLongPosition(candle);
        }

        // Закрытие всех LONG ордеров
        if (type == TYPE.SHORT && longIsOpen && !shortIsOpen && openOrders > 0) {
            closeLongPosition(candle);
        }

        // Открытие первого SHORT ордера при выполнении условий
        if (type == TYPE.SHORT && !shortIsOpen && !longIsOpen) {
            openShortPosition(candle);
        }

        // Закрытие всех SHORT ордеров
        if (type == TYPE.LONG && !longIsOpen && shortIsOpen && openOrders > 0) {
            closeShortPosition(candle);
        }
    }

    /**
     * Метод для открытия LONG позиции
     */
    private void openLongPosition(Candle candle) {
        // Пересчет маржи для следующего пула сделок
        calculateInitialMarginPerOrder();
        if (currentDeposit < marginPerOrder) {
            return;
        }

        openOrders++;

        position = new Position(tradingPair, TYPE.LONG, LEVERAGE);
        Order order = new Order(tradingPair, "buy", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        last_long_price = currentPrice;
        longIsOpen = true;
    }

    /**
     * Метод для закрытия всех LONG позиций
     */
    private void closeLongPosition(Candle candle) {
        // Закрытие позиции
        Order order = new Order(tradingPair, "sell", candle.getTime(), marginQTY * openOrders, currentPrice, STATUS.CLOSE);
        position.closePosition(order);
        orderHistory.add(order);

        // Обновление депозита
        double profit = position.getProfit();
        currentDeposit += profit;
        // Обновление состояния стратегии
        longIsOpen = false;
        openOrders = 0;
        positionHistory.add(position);
    }

    /**
     * Метод для открытия SHORT позиции
     */
    private void openShortPosition(Candle candle) {
        // Пересчет маржи для следующего пула сделок
        calculateInitialMarginPerOrder();

        if (currentDeposit < marginPerOrder) {
            return;
        }

        openOrders++;

        position = new Position(tradingPair, TYPE.SHORT, LEVERAGE);
        Order order = new Order(tradingPair, "sell", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        last_short_price = currentPrice;
        shortIsOpen = true;
    }

    /**
     * Метод для закрытия всех SHORT позиций
     */
    private void closeShortPosition(Candle candle) {
        // Закрытие позиции
        Order order = new Order(tradingPair, "buy", candle.getTime(), marginQTY * openOrders, currentPrice, STATUS.CLOSE);
        position.closePosition(order);
        orderHistory.add(order);

        // Обновление депозита
        double profit = position.getProfit();
        currentDeposit += profit;
        // Обновление состояния стратегии
        shortIsOpen = false;
        openOrders = 0;
        positionHistory.add(position);
    }
}
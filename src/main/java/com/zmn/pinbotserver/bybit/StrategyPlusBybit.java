package com.zmn.pinbotserver.bybit;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.model.order.STATUS;
import com.zmn.pinbotserver.model.order.TYPE;
import com.zmn.pinbotserver.model.strategy.StrategyParamsATR;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс StrategyPlusBybit реализует торговую стратегию с использованием индикаторов CCI и EMA, а также метода AlphaTrend.
 */
public class StrategyPlusBybit {

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

    String tradingPair; // Торговая пара

    @Getter
    List<Order> orderHistory = new ArrayList<>(); // История ордеров

    @Getter
    List<Position> positionHistory = new ArrayList<>(); // История позиций

    double initialDeposit; // Начальный депозит
    double risk; // Риск на сделку

    double currentDeposit; // Текущий депозит

    double marginPerOrder; // Маржа на сделку
    double marginQTY; // Количество монет на сделку
    double minTradingQty; // Минимальное количество монет для торговли

    int ATR_length; // Период для расчета ATR
    double coeff; // Множитель для AlphaTrend

    // Поля для AlphaTrend
    double alphaTrend = 0.0;
    double prevAlphaTrend = 0.0;
    boolean alphaTrendBuy = false;
    boolean alphaTrendSell = false;

    /**
     * Конструктор стратегии
     *
     * @param strategyParams   параметры стратегии
     * @param initialDeposit   начальный депозит
     * @param minTradingQty    минимальное количество монет для торговли
     * @param risk             риск на сделку
     */
    public StrategyPlusBybit(StrategyParamsATR strategyParams, double initialDeposit, double minTradingQty, double risk) {
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
        this.ATR_length = strategyParams.getATR_Length();
        this.coeff = strategyParams.getCoeff();
        calculateInitialMarginPerOrder();
    }

    /**
     * Метод для расчета маржи на сделку и соответствующего количества монет
     */
    private void calculateInitialMarginPerOrder() {
        this.marginQTY = (currentDeposit * (risk / 100) * LEVERAGE) / (MAXOrders * currentPrice);

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
    public String calcAlphaTrend(double closePrice, double low, double high, double atr, double coeff, int period) {
        double upT = low - atr * coeff;
        double downT = high + atr * coeff;

        prevAlphaTrend = alphaTrend;

        double mfi = taMFI(candleHistory, period);
        alphaTrend = (mfi >= 50) ? (upT < prevAlphaTrend ? prevAlphaTrend : upT) : (downT > prevAlphaTrend ? prevAlphaTrend : downT);

        alphaTrendBuy = taCrossover(alphaTrend, prevAlphaTrend);
        alphaTrendSell = taCrossunder(alphaTrend, prevAlphaTrend);

        if (alphaTrendBuy) {
            return "BUY";
        } else if (alphaTrendSell) {
            return "SELL";
        } else {
            return "HOLD";
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
     * Метод для расчета EMA.
     *
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
     * @return рассчитанное значение CCI
     */
    public double calculateCCI() {
        int period = CCI_PERIOD; // Период для CCI
        if (candleHistory.size() < period) {
            // Если недостаточно данных, возвращаем 0 и выводим сообщение
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
        return (candleHistory.get(candleHistory.size() - 1).getTypicalPrice() - meanTypicalPrice) / (0.015 * meanDeviation);
    }

    /**
     * Метод, вызываемый при обновлении цены.
     *
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
        String alphaTrendSignal = calcAlphaTrend(candle.getClose(), candle.getLow(), candle.getHigh(), atr, coeff, ATR_length);

        // Рассчитываем новые значения CCI и EMA
        double newCCI = calculateCCI();
        double newEMA = calculateEMA(newCCI, EMA_PERIOD);

        // Обрабатываем ордера на основе новых значений
        if (strategyAllowed) {
            manageOrders(newCCI, newEMA, candle, alphaTrendSignal);
        }
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
     * Метод для управления ордерами.
     *
     * @param cci текущее значение CCI
     * @param ema текущее значение EMA
     * @param candle текущая свеча
     * @param alphaTrendSignal сигнал AlphaTrend ("BUY", "SELL" или "HOLD")
     */
    private void manageOrders(double cci, double ema, Candle candle, String alphaTrendSignal) {
        double liquidationLevelPer = 100.0 / LEVERAGE; // Уровень ликвидации в процентах

        // Ликвидация LONG ордеров, если цена достигает уровня ликвидации
        if (longIsOpen && currentPrice <= last_long_price * (1 - liquidationLevelPer / 100)) {
            closeLongPosition(candle);
        }

        // Ликвидация SHORT ордеров, если цена достигает уровня ликвидации
        if (shortIsOpen && currentPrice >= last_short_price * (1 + liquidationLevelPer / 100)) {
            closeShortPosition(candle);
        }

        // Проверка условий для открытия первой LONG позиции
        if (alphaTrendSignal.equals("BUY") && cci < lowerBound && !longIsOpen && !longIsReadyAVG && openOrders == 0 && !longIsReady) {
            longIsReady = true; // Устанавливаем флаг готовности для открытия первого LONG
        }

        // Открытие первого LONG ордера при выполнении условий
        if (alphaTrendSignal.equals("BUY") && longIsReady && cci > ema && !longIsReadyAVG && !longIsOpen && cci <= upperBound && !shortIsOpen && !shortIsReadyAVG) {
            openLongPosition(candle);
        }

        // Установка флага, если CCI возвращается в зону
        if (cci > lowerBound) {
            cciLongRollback = true;
        }

        // Проверка условий для усреднения LONG позиции
        if (alphaTrendSignal.equals("BUY") && longIsOpen && openOrders <= MAXOrders && cciLongRollback && cci < lowerBound) {
            longIsReadyAVG = true; // Устанавливаем флаг готовности для усреднения LONG
        }

        // Открытие усредняющего LONG ордера при выполнении условий
        if (alphaTrendSignal.equals("BUY") && longIsReadyAVG && cci > ema && currentPrice < last_long_price && longIsOpen && !shortIsOpen && !shortIsReadyAVG) {
            averageLongPosition(candle);
        }

        // Закрытие всех LONG ордеров при достижении верхней границы CCI
        if (longIsOpen && !shortIsOpen && openOrders > 0 && cci > upperBound) {
            closeLongPosition(candle);
        }

        // Проверка условий для открытия первой SHORT позиции
        if (alphaTrendSignal.equals("SELL") && cci > upperBound && !shortIsOpen && !shortIsReadyAVG && openOrders == 0 && !shortIsReady) {
            shortIsReady = true; // Устанавливаем флаг готовности для открытия первого SHORT
        }

        // Открытие первого SHORT ордера при выполнении условий
        if (alphaTrendSignal.equals("SELL") && shortIsReady && cci < ema && !shortIsReadyAVG && !shortIsOpen && cci >= lowerBound && !longIsOpen && !longIsReadyAVG) {
            openShortPosition(candle);
        }

        // Установка флага, если CCI возвращается в зону
        if (cci < upperBound) {
            cciShortRollback = true;
        }

        // Проверка условий для усреднения SHORT позиции
        if (alphaTrendSignal.equals("SELL") && shortIsOpen && openOrders <= MAXOrders && cciShortRollback && cci > upperBound) {
            shortIsReadyAVG = true; // Устанавливаем флаг готовности для усреднения SHORT
        }

        // Открытие усредняющего SHORT ордера при выполнении условий
        if (alphaTrendSignal.equals("SELL") && shortIsReadyAVG && cci < ema && currentPrice > last_short_price && !longIsOpen && shortIsOpen) {
            averageShortPosition(candle);
        }

        // Закрытие всех SHORT ордеров при достижении нижней границы CCI
        if (!longIsOpen && shortIsOpen && openOrders > 0 && cci < lowerBound) {
            closeShortPosition(candle);
        }
    }

    /**
     * Метод для открытия LONG позиции.
     *
     * @param candle текущая свеча
     */
    private void openLongPosition(Candle candle) {
        // Пересчет маржи для следующего пула сделок
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
     * @param candle текущая свеча
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
     * Метод для закрытия всех LONG позиций.
     *
     * @param candle текущая свеча
     */
    private void closeLongPosition(Candle candle) {
        // Закрытие позиции
        Order order = new Order(tradingPair, "sell", candle.getTime(), marginQTY * openOrders, currentPrice, STATUS.CLOSE);
        position.closePosition(order);
        orderHistory.add(order);

        // Обновление депозита
        double profit = position.getProfit();
        currentDeposit += profit;
        //System.out.println("---- Текущий депозит: " + currentDeposit);
        // Обновление состояния стратегии
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
     * @param candle текущая свеча
     */
    private void openShortPosition(Candle candle) {
        // Пересчет маржи для следующего пула сделок
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
     * @param candle текущая свеча
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
     * Метод для закрытия всех SHORT позиций.
     *
     * @param candle текущая свеча
     */
    private void closeShortPosition(Candle candle) {
        // Закрытие позиции
        Order order = new Order(tradingPair, "buy", candle.getTime(), marginQTY * openOrders, currentPrice, STATUS.CLOSE);
        position.closePosition(order);
        orderHistory.add(order);

        // Обновление депозита
        double profit = position.getProfit();
        currentDeposit += profit;
        //System.out.println("---- Текущий депозит: " + currentDeposit);
        // Обновление состояния стратегии
        shortIsOpen = false;
        shortIsReady = false;
        shortIsReadyAVG = false;
        openOrders = 0;
        positionHistory.add(position);
    }
}

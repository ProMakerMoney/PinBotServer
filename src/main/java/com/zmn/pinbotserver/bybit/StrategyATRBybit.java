package com.zmn.pinbotserver.bybit;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.model.order.STATUS;
import com.zmn.pinbotserver.model.order.TYPE;

import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StrategyATRBybit {

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

    @Getter
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
    String mode = "added"; // режим по умолчанию

    BybitApi api = new BybitApi("bvQRWwQU8QapNl3Ppl", "P5h8tnabkftRGzrdFV4DXbggI7XJnaaXx6KY", false);


    /**
     * Конструктор стратегии ATR.
     *
     * @param strategyParams Параметры стратегии.
     * @param initialDeposit Начальный депозит.
     * @param minTradingQty  Минимальное количество для торговли.
     * @param risk           Уровень риска.
     */
    public StrategyATRBybit(StrategyParamsBybit strategyParams, double initialDeposit, double minTradingQty, double risk) {
        this.tradingPair = strategyParams.getCoinName();
        this.LEVERAGE = strategyParams.getLeverage();
        this.CCI_PERIOD = strategyParams.getCCI();
        this.EMA_PERIOD = strategyParams.getEMA();
        this.upperBound = 100 * strategyParams.getRatio();
        this.lowerBound = -100 * strategyParams.getRatio();
        this.MAXOrders = strategyParams.getMaxOrders();
        this.initialDeposit = initialDeposit;
        this.currentDeposit = initialDeposit;
        this.minTradingQty = minTradingQty;
        this.risk = risk;
        this.ATR_length = strategyParams.getATR();
        this.coeff = strategyParams.getCoeff();
        calculateInitialMarginPerOrder();
    }

    /**
     * Метод для расчета маржи на сделку.
     */
    private void calculateInitialMarginPerOrder() {
        this.marginQTY = (currentDeposit * (risk / 100) * LEVERAGE) / (MAXOrders * currentPrice);
        this.marginQTY = Math.floor(this.marginQTY * 10) / 10.0;

        if (this.marginQTY < this.minTradingQty) {
            if (this.marginQTY >= this.minTradingQty * 0.9) {
                this.marginQTY = this.minTradingQty;
                strategyAllowed = true;
            } else {
                strategyAllowed = false;
            }
        }
    }

    /**
     * Метод для расчета AlphaTrend индикатора.
     *
     * @param low    Текущая минимальная цена.
     * @param high   Текущая максимальная цена.
     * @param atr    Значение ATR.
     * @param coeff  Множитель для AlphaTrend.
     * @param period Период для AlphaTrend.
     */
    public void calcAlphaTrend(double low, double high, double atr, double coeff, int period) {
        double upT = low - atr * coeff;
        double downT = high + atr * coeff;
        prevAlphaTrend = alphaTrend;
        double mfi = taMFI(candleHistory, period);
        alphaTrend = (mfi >= 50) ? (Math.max(upT, prevAlphaTrend)) : (Math.min(downT, prevAlphaTrend));
        alphaTrendBuy = taCrossover(alphaTrend, prevAlphaTrend);
        alphaTrendSell = taCrossunder(alphaTrend, prevAlphaTrend);
        if (alphaTrendBuy) {
            direction = TYPE.LONG;
        } else if (alphaTrendSell) {
            direction = TYPE.SHORT;
        }
    }

    /**
     * Метод для определения пересечения снизу.
     *
     * @param current  Текущее значение.
     * @param previous Предыдущее значение.
     * @return true, если произошло пересечение снизу, иначе false.
     */
    public boolean taCrossover(double current, double previous) {
        return current > previous;
    }

    /**
     * Метод для определения пересечения сверху.
     *
     * @param current  Текущее значение.
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
     * @param period        Период для MFI.
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
        double moneyFlowRatio = positiveMoneyFlow / negativeMoneyFlow;
        return 100 - (100 / (1 + moneyFlowRatio));
    }

    /**
     * Метод для расчета Average True Range (ATR).
     *
     * @param candleHistory Список свечей.
     * @param ATR_length    Период для ATR.
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
     * @param period   Период EMA.
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
     *
     */
    public void onPriceUpdate() {

        Candle candle = api.getCandle(tradingPair, "15");

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
            candleHistory.removeFirst();
        }
        double atr = calculateATR(candleHistory, ATR_length);
        calcAlphaTrend(candle.getLow(), candle.getHigh(), atr, coeff, ATR_length);
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

    private void checkFirstLongReady(double cci) {
        if (cci < lowerBound && !longIsOpen && !longIsReadyAVG && openOrders == 0 && !longIsReady) {
            longIsReady = true;
            log("Готовность открытия первой LONG позиции.");
        }
    }

    private void checkFirstShortReady(double cci) {
        if (cci > upperBound && !shortIsOpen && !shortIsReadyAVG && openOrders == 0 && !shortIsReady) {
            shortIsReady = true;
            log("Готовность открытия первой SHORT позиции.");
        }
    }

    private void checkLongAverageReady(double cci) {
        if (direction == TYPE.LONG && longIsOpen && openOrders <= MAXOrders && cci > lowerBound) {
            cciLongRollback = true;
        }
        if (direction == TYPE.LONG && longIsOpen && openOrders <= MAXOrders && cciLongRollback && cci < lowerBound) {
            longIsReadyAVG = true;
            log("Готовность усреднения LONG позиции.");
        }
    }

    private void checkShortAverageReady(double cci) {
        if (direction == TYPE.SHORT && shortIsOpen && openOrders <= MAXOrders && cci < upperBound) {
            cciShortRollback = true;
        }
        if (direction == TYPE.SHORT && shortIsOpen && openOrders <= MAXOrders && cciShortRollback && cci > upperBound) {
            shortIsReadyAVG = true;
            log("Готовность усреднения SHORT позиции.");
        }
    }

    private boolean canOpenFirstLongPosition(double cci, double ema) {
        boolean canOpen = direction == TYPE.LONG && longIsReady && cci > ema && !longIsReadyAVG && !longIsOpen && cci <= upperBound && !shortIsOpen && !shortIsReadyAVG && openOrders < MAXOrders;
        if (canOpen) {
            log("Условия для открытия первой LONG позиции выполнены.");
        }
        return canOpen;
    }

    private boolean canOpenFirstShortPosition(double cci, double ema) {
        boolean canOpen = direction == TYPE.SHORT && shortIsReady && cci < ema && !shortIsReadyAVG && !shortIsOpen && cci >= lowerBound && !longIsOpen && !longIsReadyAVG  && openOrders < MAXOrders;
        if (canOpen) {
            log("Условия для открытия первой SHORT позиции выполнены.");
        }
        return canOpen;
    }

    private boolean canAverageLongPosition(double cci, double ema) {
        boolean canAverage = direction == TYPE.LONG && longIsReadyAVG && cci > ema && currentPrice < last_long_price && longIsOpen && !shortIsOpen && !shortIsReadyAVG  && openOrders < MAXOrders;
        if (canAverage) {
            log("Условия для усреднения LONG позиции выполнены.");
        }
        return canAverage;
    }

    private boolean canAverageShortPosition(double cci, double ema) {
        boolean canAverage = direction == TYPE.SHORT && shortIsReadyAVG && cci < ema && currentPrice > last_short_price && !longIsOpen && shortIsOpen  && openOrders < MAXOrders;
        if (canAverage) {
            log("Условия для усреднения SHORT позиции выполнены.");
        }
        return canAverage;
    }

    private boolean canCloseLongPosition(double cci) {
        double liquidationLevelPer = 100.0 / LEVERAGE; // Уровень ликвидации в процентах
        boolean canClose = (longIsOpen && (currentPrice <= last_long_price * (1 - liquidationLevelPer / 100)) ||
                (cci > upperBound && longIsOpen && openOrders > 0));
        if (canClose) {
            log("Условия для закрытия LONG позиции выполнены.");
        }
        return canClose;
    }

    private boolean canCloseShortPosition(double cci) {
        double liquidationLevelPer = 100.0 / LEVERAGE; // Уровень ликвидации в процентах
        boolean canClose = (shortIsOpen && (currentPrice >= last_short_price * (1 + liquidationLevelPer / 100)) ||
                (cci < lowerBound && shortIsOpen && openOrders > 0));
        if (canClose) {
            log("Условия для закрытия SHORT позиции выполнены.");
        }
        return canClose;
    }

    private void openLongPosition(Candle candle) {
        calculateInitialMarginPerOrder();
        if (currentDeposit < marginPerOrder * MAXOrders) {
            return;
        }
        openOrders++;

        if(Objects.equals(mode, "activate")) {
            try {
                api.placeOrder(tradingPair, "Buy", "Market", String.valueOf(marginQTY), null);
            } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log("Биржа деактивирована");
        }

        position = new Position(tradingPair, TYPE.LONG, LEVERAGE);
        Order order = new Order(tradingPair, "buy", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        last_long_price = currentPrice;
        longIsReady = false;
        longIsOpen = true;
        log("Открыта LONG позиция по цене: " + candle);
    }

    private void averageLongPosition(Candle candle) {
        if (currentDeposit < marginPerOrder) {
            return;
        }
        openOrders++;

        if(Objects.equals(mode, "activate")) {
            try {
                api.placeOrder(tradingPair, "Buy", "Market", String.valueOf(marginQTY), null);
            } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log("Биржа деактивирована");
        }

        Order order = new Order(tradingPair, "buy", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        last_long_price = currentPrice;
        longIsReadyAVG = false;
        cciLongRollback = false;
        log("Усреднена LONG позиция по цене: " + currentPrice);
    }

    private void closeLongPosition(Candle candle) {

        if(Objects.equals(mode, "activate")) {
            try {
                api.placeOrder(tradingPair, "Sell", "Market", String.valueOf(marginQTY * openOrders), null);
            } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log("Биржа деактивирована");
        }

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
        log("Закрыта LONG позиция по цене: " + currentPrice + "Прибыль: " + profit);
    }

    private void openShortPosition(Candle candle) {
        calculateInitialMarginPerOrder();
        if (currentDeposit < marginPerOrder * MAXOrders) {
            return;
        }
        openOrders++;

        if(Objects.equals(mode, "activate")) {
            try {
                api.placeOrder(tradingPair, "Sell", "Market", String.valueOf(marginQTY), null);
            } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log("Биржа деактивирована");
        }

        position = new Position(tradingPair, TYPE.SHORT, LEVERAGE);
        Order order = new Order(tradingPair, "sell", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        last_short_price = currentPrice;
        shortIsReady = false;
        shortIsOpen = true;
        log("Открыта SHORT позиция по цене: " + currentPrice);
    }

    private void averageShortPosition(Candle candle) {
        if (currentDeposit < marginPerOrder) {
            return;
        }
        openOrders++;

        if(Objects.equals(mode, "activate")) {
            try {
                api.placeOrder(tradingPair, "Sell", "Market", String.valueOf(marginQTY), null);
            } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log("Биржа деактивирована");
        }

        Order order = new Order(tradingPair, "sell", candle.getTime(), marginQTY, currentPrice, STATUS.OPEN);
        position.addOrder(order);
        orderHistory.add(order);
        last_short_price = currentPrice;
        shortIsReadyAVG = false;
        cciShortRollback = false;
        log("Усреднена SHORT позиция по цене: " + currentPrice);
    }

    private void closeShortPosition(Candle candle) {

        if(Objects.equals(mode, "activate")) {
            try {
                api.placeOrder(tradingPair, "Buy", "Market", String.valueOf(marginQTY * openOrders), null);
            } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log("Биржа деактивирована");
        }

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
        log("Закрыта SHORT позиция по цене: " + currentPrice + "Прибыль: " + profit);
    }

    private void log(String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println(tradingPair + " - " + timestamp + " - " + message);
    }

    public void getCurrentCandles() throws IOException {
        String fileName = tradingPair + "_" + "15" + "_history.csv";
        Path filePath = Paths.get("historical_data", fileName);
        List<Candle> candles = readCandlesFromCsv(filePath);
        int candleCount = 300; // 300 свечей
        int startIndex = Math.max(candles.size() - candleCount, 0);
        List<Candle> recentCandles = candles.subList(startIndex, candles.size());
        candleHistory = recentCandles;

        System.out.println("Загружено свечей: " + recentCandles.size());

        for (Candle candle : candleHistory) {
            double atr = calculateATR(candleHistory, ATR_length);
            calcAlphaTrend(candle.getLow(), candle.getHigh(), atr, coeff, ATR_length);
            double newCCI = calculateCCI();
            double newEMA = calculateEMA(newCCI, EMA_PERIOD);



        }
    }

    /**
     * Метод для чтения свечей из CSV файла
     * @param filePath путь к файлу
     * @return список свечей
     * @throws IOException возможное исключение ввода-вывода
     */
    public List<Candle> readCandlesFromCsv(Path filePath) throws IOException {
        List<Candle> candles = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            reader.readLine(); // Пропускаем заголовок
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                Candle candle = new Candle(
                        Long.parseLong(fields[0]),
                        Double.parseDouble(fields[1]),
                        Double.parseDouble(fields[2]),
                        Double.parseDouble(fields[3]),
                        Double.parseDouble(fields[4]),
                        Double.parseDouble(fields[5]),
                        Double.parseDouble(fields[6])
                );
                candles.add(candle);
            }
        }
        return candles;
    }
}
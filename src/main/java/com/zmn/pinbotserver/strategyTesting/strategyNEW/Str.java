package com.zmn.pinbotserver.strategyTesting.strategyNEW;


import com.zmn.pinbotserver.model.candle.Candle;

import java.util.LinkedList;
import java.util.List;

public class Str {

    private final LinkedList<Double> emaValues1 = new LinkedList<>();
    private final LinkedList<Double> emaValues2 = new LinkedList<>();
    private final LinkedList<Double> emaValues3 = new LinkedList<>();
    private final LinkedList<Double> smaValues = new LinkedList<>();

    private int atrPeriod = 1;
    private double atrMultiplier = 2.0;

    private List<Candle> candles = new LinkedList<>();
    private LinkedList<Double> xATRTrailingStops = new LinkedList<>();
    private double atr;

    private String lastSignal = "hold"; // Хранение последнего сигнала

    double sma;

    // Общий метод для расчета EMA
    private double calculateEMA(double newValue, int period, LinkedList<Double> emaValues) {
        if (emaValues.isEmpty()) {
            emaValues.add(newValue);
            return newValue;
        } else {
            double lastEma = emaValues.getLast();
            double alpha = 2.0 / (period + 1);
            double ema = alpha * newValue + (1 - alpha) * lastEma;
            emaValues.add(ema);
            return ema;
        }
    }

    // Метод для расчета SMA
    public double calculateSMA(double newValue, int period) {
        smaValues.add(newValue);
        if (smaValues.size() > period) {
            smaValues.removeFirst();
        }

        double sum = 0.0;
        for (double value : smaValues) {
            sum += value;
        }

        return sum / smaValues.size();
    }

    // Метод для расчета ATR
    private double calculateATR(int period, int index) {
        double sum = 0.0;
        for (int i = 0; i < period; i++) {
            Candle currentCandle = candles.get(index - i);
            Candle previousCandle = candles.get(index - i - 1);
            double highLow = currentCandle.getHigh() - currentCandle.getLow();
            double highClose = Math.abs(currentCandle.getHigh() - previousCandle.getClose());
            double lowClose = Math.abs(currentCandle.getLow() - previousCandle.getClose());
            sum += Math.max(highLow, Math.max(highClose, lowClose));
        }
        return sum / period;
    }

    public String getSignal(Candle newCandle) {
        Candle heikinAshi = calculateHeikinAshi(newCandle);
        candles.add(heikinAshi);
        int length = candles.size();
        if (length < 20) {
            return "not enough data";
        }

        if (length > atrPeriod + 1) {
            atr = calculateATR(atrPeriod, length - 1);
        }

        double nLoss = atr * atrMultiplier;
        double src = newCandle.getClose();
        double prevSrc = candles.get(length - 2).getClose();
        double prevXATRTrailingStop = (xATRTrailingStops.size() > 0) ? xATRTrailingStops.getLast() : src;

        double newXATRTrailingStop;
        if (src > prevXATRTrailingStop && prevSrc > prevXATRTrailingStop) {
            newXATRTrailingStop = Math.max(prevXATRTrailingStop, src - nLoss);
        } else if (src < prevXATRTrailingStop && prevSrc < prevXATRTrailingStop) {
            newXATRTrailingStop = Math.min(prevXATRTrailingStop, src + nLoss);
        } else if (src > prevXATRTrailingStop) {
            newXATRTrailingStop = src - nLoss;
        } else {
            newXATRTrailingStop = src + nLoss;
        }

        xATRTrailingStops.add(newXATRTrailingStop);

        double ema1 = calculateEMA(heikinAshi.getHL2(), 9, emaValues1);
        double ema2 = calculateEMA(ema1, 9, emaValues2);
        double ema3 = calculateEMA(ema2, 9, emaValues3);

        double out = 3 * (ema1 - ema2) + ema3;
        sma = calculateSMA(out, 10);

        boolean above = src > newXATRTrailingStop && out > newXATRTrailingStop;
        boolean below = src < newXATRTrailingStop && out < newXATRTrailingStop;

        System.out.printf("Candle: %s, SMA: %.4f, ATR: %.4f, xATRTrailingStop: %.4f, Above: %b, Below: %b\n",
                heikinAshi, sma, atr, newXATRTrailingStop, above, below);

        if (above && !lastSignal.equals("buy")) {
            lastSignal = "buy";
            return "buy";
        } else if (below && !lastSignal.equals("sell")) {
            lastSignal = "sell";
            return "sell";
        } else {
            return "hold";
        }
    }

    private Candle calculateHeikinAshi(Candle newCandle) {
        if (candles.isEmpty()) {
            // Если это первая свеча Хейкен-Аши, она совпадает с первой обычной свечой
            return newCandle;
        }

        // Получаем последнюю рассчитанную Хейкен-Аши свечу
        Candle previousHaCandle = candles.get(candles.size() - 1);

        // Рассчитываем цену закрытия для Хейкен-Аши
        double haClose = (newCandle.getOpen() + newCandle.getHigh() + newCandle.getLow() + newCandle.getClose()) / 4;

        // Рассчитываем цену открытия для Хейкен-Аши
        double haOpen = (previousHaCandle.getOpen() + previousHaCandle.getClose()) / 2;

        // Рассчитываем цену максимума для Хейкен-Аши
        double haHigh = Math.max(newCandle.getHigh(), Math.max(haOpen, haClose));

        // Рассчитываем цену минимума для Хейкен-Аши
        double haLow = Math.min(newCandle.getLow(), Math.min(haOpen, haClose));

        // Возвращаем новую свечу Хейкен-Аши
        return new Candle(newCandle.getTime(), haHigh, haLow, haOpen, haClose, newCandle.getVolume(), newCandle.getQuoteVolume());
    }


    public double getLastSMA() {
        return sma;
    }

}
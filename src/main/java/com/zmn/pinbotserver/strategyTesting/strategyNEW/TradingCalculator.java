package com.zmn.pinbotserver.strategyTesting.strategyNEW;

import java.util.List;

/**
 * Класс для вычислений торговых индикаторов.
 */
public class TradingCalculator {

    /**
     * Вычисляет линейную регрессию для заданных данных.
     * @param data список значений
     * @param length длина периода регрессии
     * @return значение линейной регрессии
     */
    public static double linreg(List<Double> data, int length) {
        int n = data.size();
        if (n < length) {
            throw new IllegalArgumentException("Not enough data points");
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < length; i++) {
            sumX += i;
            sumY += data.get(n - length + i);
            sumXY += i * data.get(n - length + i);
            sumX2 += i * i;
        }

        double slope = (length * sumXY - sumX * sumY) / (length * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / length;

        return slope * (length - 1) + intercept;
    }

    /**
     * Вычисляет SMA (Simple Moving Average).
     * @param data список значений
     * @param length длина периода скользящего среднего
     * @return значение SMA
     */
    public static double sma(List<Double> data, int length) {
        int n = data.size();
        if (n < length) {
            throw new IllegalArgumentException("Not enough data points");
        }

        double sum = 0;
        for (int i = n - length; i < n; i++) {
            sum += data.get(i);
        }

        return sum / length;
    }

    /**
     * Вычисляет EMA (Exponential Moving Average).
     * @param data список значений
     * @param length длина периода скользящего среднего
     * @return значение EMA
     */
    public static double ema(List<Double> data, int length) {
        int n = data.size();
        if (n < length) {
            throw new IllegalArgumentException("Not enough data points");
        }

        double multiplier = 2.0 / (length + 1);
        double ema = sma(data.subList(0, length), length);

        for (int i = length; i < n; i++) {
            ema = (data.get(i) - ema) * multiplier + ema;
        }

        return ema;
    }

    /**
     * Вычисляет ATR (Average True Range).
     * @param high список максимальных цен
     * @param low список минимальных цен
     * @param close список цен закрытия
     * @param length длина периода ATR
     * @return значение ATR
     */
    public static double atr(List<Double> high, List<Double> low, List<Double> close, int length) {
        int n = high.size();
        if (n < length) {
            throw new IllegalArgumentException("Not enough data points");
        }

        double sumTR = 0;
        for (int i = 1; i < n; i++) {
            double tr = Math.max(high.get(i) - low.get(i),
                    Math.max(Math.abs(high.get(i) - close.get(i - 1)),
                            Math.abs(low.get(i) - close.get(i - 1))));
            sumTR += tr;
        }

        return sumTR / length;
    }
}


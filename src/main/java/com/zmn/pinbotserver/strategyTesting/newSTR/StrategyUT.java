package com.zmn.pinbotserver.strategyTesting.newSTR;

import com.zmn.pinbotserver.model.candle.Candle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class StrategyUT {

    // Параметры для UT Bot Alerts
    private int utBotKey;
    private int atrPeriod;
    private boolean useHeikinAshi;

    // Параметры для Humble LinReg Candles
    private int signalLength;
    private boolean useSMA;
    private int linRegLength;

    private static final int MINIMUM_CANDLES = 20;

    LinkedList<Candle> candleHistory = new LinkedList<>(); // История свечей

    // Конструктор для установки параметров
    public StrategyUT(int utBotKey, int atrPeriod, boolean useHeikinAshi, int signalLength, boolean useSMA, int linRegLength) {
        this.utBotKey = utBotKey;
        this.atrPeriod = atrPeriod;
        this.useHeikinAshi = useHeikinAshi;
        this.signalLength = signalLength;
        this.useSMA = useSMA;
        this.linRegLength = linRegLength;
    }

    // Метод для UT Bot Alerts
    public static void utBotAlerts(double[] closePrices, double[] highPrices, double[] lowPrices, int a, int c, boolean useHeikinAshi) {

        double[] atr = calculateATR(highPrices, lowPrices, closePrices, c);
        double[] xATRTrailingStop = new double[closePrices.length];
        double nLoss;
        int pos = 0;

        for (int i = 1; i < closePrices.length; i++) {
            double src = useHeikinAshi ? heikinAshi(closePrices, highPrices, lowPrices)[i] : closePrices[i];
            nLoss = a * atr[i];

            xATRTrailingStop[i] = (src > xATRTrailingStop[i - 1] && closePrices[i - 1] > xATRTrailingStop[i - 1]) ? Math.max(xATRTrailingStop[i - 1], src - nLoss)
                    : (src < xATRTrailingStop[i - 1] && closePrices[i - 1] < xATRTrailingStop[i - 1]) ? Math.min(xATRTrailingStop[i - 1], src + nLoss)
                    : (src > xATRTrailingStop[i - 1]) ? src - nLoss : src + nLoss;

            pos = (closePrices[i - 1] < xATRTrailingStop[i - 1] && closePrices[i] > xATRTrailingStop[i]) ? 1
                    : (closePrices[i - 1] > xATRTrailingStop[i - 1] && closePrices[i] < xATRTrailingStop[i]) ? -1 : pos;

            boolean buy = (closePrices[i] > xATRTrailingStop[i]) && (ema(closePrices, 1)[i] > xATRTrailingStop[i]);
            boolean sell = (closePrices[i] < xATRTrailingStop[i]) && (ema(closePrices, 1)[i] < xATRTrailingStop[i]);

            if (buy) {

            } else if (sell) {

            }
        }

    }

    // Метод для Humble LinReg Candles
    public static double[] humbleLinRegCandles(double[] openPrices, double[] highPrices, double[] lowPrices, double[] closePrices, int signalLength, boolean useSMA, int linRegLength) {
        double[] bclose = linreg(closePrices, linRegLength);
        double[] signal = useSMA ? sma(bclose, signalLength) : ema(bclose, signalLength);
        return signal;
    }

    // Вспомогательные методы

    // Расчет ATR
    public static double[] calculateATR(double[] highPrices, double[] lowPrices, double[] closePrices, int period) {
        double[] atr = new double[closePrices.length];
        for (int i = period; i < closePrices.length; i++) {
            double sumTR = 0;
            for (int j = i - period + 1; j <= i; j++) {
                double tr = Math.max(highPrices[j] - lowPrices[j], Math.max(Math.abs(highPrices[j] - closePrices[j - 1]), Math.abs(lowPrices[j] - closePrices[j - 1])));
                sumTR += tr;
            }
            atr[i] = sumTR / period;
        }
        return atr;
    }

    // Расчет EMA
    public static double[] ema(double[] prices, int period) {
        double[] ema = new double[prices.length];
        double multiplier = 2.0 / (period + 1);
        ema[0] = prices[0];
        for (int i = 1; i < prices.length; i++) {
            ema[i] = ((prices[i] - ema[i - 1]) * multiplier) + ema[i - 1];
        }
        return ema;
    }

    // Расчет SMA
    public static double[] sma(double[] prices, int period) {
        double[] sma = new double[prices.length];
        for (int i = period - 1; i < prices.length; i++) {
            double sum = 0;
            for (int j = i - period + 1; j <= i; j++) {
                sum += prices[j];
            }
            sma[i] = sum / period;
        }
        return sma;
    }

    // Расчет Linear Regression
    public static double[] linreg(double[] prices, int period) {
        double[] linreg = new double[prices.length];
        for (int i = period - 1; i < prices.length; i++) {
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            for (int j = 0; j < period; j++) {
                sumX += j + 1;
                sumY += prices[i - j];
                sumXY += (j + 1) * prices[i - j];
                sumX2 += (j + 1) * (j + 1);
            }
            double slope = (period * sumXY - sumX * sumY) / (period * sumX2 - sumX * sumX);
            double intercept = (sumY - slope * sumX) / period;
            linreg[i] = slope * period + intercept;
        }
        return linreg;
    }

    // Расчет Heikin Ashi (пример)
    public static double[] heikinAshi(double[] closePrices, double[] highPrices, double[] lowPrices) {
        double[] haClose = new double[closePrices.length];
        for (int i = 0; i < closePrices.length; i++) {
            haClose[i] = (closePrices[i] + highPrices[i] + lowPrices[i]) / 3;
        }
        return haClose;
    }

    /**
     * Метод, вызываемый при обновлении цены.
     *
     * @param candle Новая свеча.
     */
    public void onPriceUpdate(Candle candle) {
        if (!candleHistory.contains(candle)) {
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

        double[] openPrices = candleHistory.stream().mapToDouble(Candle::getOpen).toArray();
        double[] highPrices = candleHistory.stream().mapToDouble(Candle::getHigh).toArray();
        double[] lowPrices = candleHistory.stream().mapToDouble(Candle::getLow).toArray();
        double[] closePrices = candleHistory.stream().mapToDouble(Candle::getClose).toArray();

        utBotAlerts(closePrices, highPrices, lowPrices, utBotKey, atrPeriod, useHeikinAshi);
        double[] humbleLinRegSignal = humbleLinRegCandles(openPrices, highPrices, lowPrices, closePrices, signalLength, useSMA, linRegLength);
 

    }
}

package com.zmn.pinbotserver.strategyTesting.strategyNEW;


import com.zmn.pinbotserver.model.candle.Candle;

import java.util.ArrayList;
import java.util.List;

public class Str {


    private int linregLength = 11;
    private int signalLength = 10;
    private boolean smaSignal = true;
    private int atrPeriod = 1;
    private double atrMultiplier = 2.0;

    private List<Candle> candles = new ArrayList<>();

    private List<Double> xATRTrailingStops = new ArrayList<>();
    private double atr;

        private double calculateSMA(int period, int index) {
            double sum = 0.0;
            for (int i = 0; i < period; i++) {
                sum += candles.get(index - i).getClose();
            }
            return sum / period;
        }

        private double calculateEMA(int period, int index) {
            double alpha = 2.0 / (period + 1);
            double ema = candles.get(index).getClose();
            for (int i = index - 1; i >= index - period + 1; i--) {
                ema = alpha * candles.get(i).getClose() + (1 - alpha) * ema;
            }
            return ema;
        }

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

        private double[] calculateLinReg(int period) {
            double[] result = new double[candles.size()];
            for (int i = period - 1; i < candles.size(); i++) {
                double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
                for (int j = 0; j < period; j++) {
                    sumX += j;
                    sumY += candles.get(i - j).getClose();
                    sumXY += j * candles.get(i - j).getClose();
                    sumX2 += j * j;
                }
                double slope = (period * sumXY - sumX * sumY) / (period * sumX2 - sumX * sumX);
                double intercept = (sumY - slope * sumX) / period;
                result[i] = slope * (period - 1) + intercept;
            }
            return result;
        }

    public String getSignal(Candle newCandle) {
        candles.add(newCandle);
        int length = candles.size();
        if (length < linregLength || length < signalLength || length < atrPeriod + 1) {
            return "not enough data";
        }

        if (length > atrPeriod + 1) {
            atr = calculateATR(atrPeriod, length - 1);
        }

        double nLoss = atr * atrMultiplier;
        double src = candles.get(length - 1).getClose();
        double prevSrc = candles.get(length - 2).getClose();
        double prevXATRTrailingStop = (xATRTrailingStops.size() > 0) ? xATRTrailingStops.get(xATRTrailingStops.size() - 1) : src;

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

        double ema = calculateEMA(1, length - 1);
        boolean above = src > newXATRTrailingStop && ema > newXATRTrailingStop;
        boolean below = src < newXATRTrailingStop && ema < newXATRTrailingStop;

        System.out.printf("Candle: %s, ATR: %.4f, xATRTrailingStop: %.4f, EMA: %.4f, Above: %b, Below: %b\n",
                newCandle, atr, newXATRTrailingStop, ema, above, below);

        if (above) {
            return "buy";
        } else if (below) {
            return "sell";
        } else {
            return "hold";
        }
    }
}


package com.zmn.pinbotserver.strategyTesting;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.TYPE;

import java.util.ArrayList;
import java.util.List;

import com.zmn.pinbotserver.model.order.Position;
import com.zmn.pinbotserver.model.order.STATUS;
import lombok.Getter;


public class AlphaTrendStrategy {

    private boolean strategyAllowed = true; // Флаг, разрешающий или запрещающий работу стратегии


    String tradingPair;

    double currentPrice; // Текущая цена инструмента

    // Параметры стратегии
    private double coeff; // Коэффициент мультипликатора для ATR
    private int AP; // Общий период для расчета ATR
    int LEVERAGE; // Плечо для торговли


    private double lastAlphaTrend = 0; // Значение AlphaTrend на предыдущем шаге


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


    Position position;
    int openOrders = 0;

    // Конструктор для инициализации параметров стратегии
    public AlphaTrendStrategy(double initialDeposit, double coeff, int AP) {
        this.initialDeposit = initialDeposit;
        this.coeff = coeff;
        this.AP = AP;
    }

    // Метод для расчета ATR (Average True Range)
    private double calculateATR(List<Candle> candles) {
        double sumTR = 0;
        for (int i = 1; i <= AP; i++) {
            Candle current = candles.get(candles.size() - i);
            Candle previous = candles.get(candles.size() - i - 1);
            double tr = Math.max(current.getHigh() - current.getLow(),
                    Math.max(Math.abs(current.getHigh() - previous.getClose()),
                            Math.abs(current.getLow() - previous.getClose())));
            sumTR += tr;
        }
        return sumTR / AP; // Среднее значение TR за период AP
    }

    // Метод для расчета AlphaTrend
    private double calculateAlphaTrend(List<Candle> candles) {
        double ATR = calculateATR(candles); // Расчет ATR
        Candle lastCandle = candles.getLast();
        double upT = lastCandle.getLow() - ATR * coeff; // Верхняя граница
        double downT = lastCandle.getHigh() + ATR * coeff; // Нижняя граница

        // Условие для расчета AlphaTrend в зависимости от закрытия свечи
        if (lastCandle.getClose() >= 50) {
            return Math.max(upT, lastAlphaTrend);
        } else {
            return Math.min(downT, lastAlphaTrend);
        }
    }

    // Метод, который вызывается при поступлении новых данных (тик)
    public void onTick(Candle candle, List<Candle> candles) {
        double AlphaTrend = calculateAlphaTrend(candles); // Расчет текущего AlphaTrend

        // Проверка условий для покупки или продажи
        if (AlphaTrend > lastAlphaTrend) {
            // Сигнал на покупку
            System.out.println("Buy signal generated.");
        } else if (AlphaTrend < lastAlphaTrend) {
            // Сигнал на продажу
            System.out.println("Sell signal generated.");
        }

        lastAlphaTrend = AlphaTrend; // Обновление последнего значения AlphaTrend
    }

    /**
     * Метод для расчета маржи на сделку и соответствующего количества монет
     */
    private void calculateInitialMarginPerOrder() {
        this.marginQTY = (currentDeposit * (risk / 100) * LEVERAGE) / (currentPrice);

        // Округляем marginQTY в меньшую сторону до одного знака после запятой
        this.marginQTY = Math.floor(this.marginQTY * 10) / 10.0;

//        if(marginQTY < minTradingQty){
//            this.marginQTY = minTradingQty;
//        }

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
        //System.out.println("---- Текущий депозит: " + currentDeposit);
        // Обновление состояния стратегии

        openOrders = 0;
        positionHistory.add(position);
    }
}

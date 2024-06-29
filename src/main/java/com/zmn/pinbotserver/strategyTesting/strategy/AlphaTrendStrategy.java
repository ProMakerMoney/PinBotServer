package com.zmn.pinbotserver.strategyTesting.strategy;

import com.zmn.pinbotserver.historicalData.model.candle.Candle;
import com.zmn.pinbotserver.strategyTesting.model.order.Order;
import com.zmn.pinbotserver.strategyTesting.model.order.TYPE;
import com.zmn.pinbotserver.strategyTesting.model.strategy.StrategyParams;

import java.util.ArrayList;
import java.util.List;

import com.zmn.pinbotserver.historicalData.model.candle.Candle;
import com.zmn.pinbotserver.strategyTesting.model.order.Order;
import com.zmn.pinbotserver.strategyTesting.model.order.Position;
import com.zmn.pinbotserver.strategyTesting.model.order.STATUS;
import com.zmn.pinbotserver.strategyTesting.model.order.TYPE;
import com.zmn.pinbotserver.strategyTesting.model.strategy.StrategyParams;
import lombok.Getter;


public class AlphaTrendStrategy {

    private boolean strategyAllowed = true; // Флаг, разрешающий или запрещающий работу стратегии

    // Параметры стратегии
    private double coeff; // Коэффициент мультипликатора для ATR
    private int AP; // Общий период для расчета ATR

    private double lastAlphaTrend = 0; // Значение AlphaTrend на предыдущем шаге

    private List<Order> openOrders = new ArrayList<>(); // Список открытых ордеров
    private List<Order> closedOrders = new ArrayList<>(); // Список закрытых ордеров

    // Конструктор для инициализации параметров стратегии
    public AlphaTrendStrategy(double coeff, int AP) {
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

    // Метод для открытия позиции
    public void openPosition(TYPE type, double price) {
        if (!strategyAllowed) return; // Проверка флага разрешения стратегии

        //Order order = new Order(type, price); // Создание нового ордера
        //openOrders.add(order); // Добавление ордера в список открытых ордеров
    }

    // Метод для закрытия позиции
    public void closePosition(Order order, double price) {
        if (!strategyAllowed) return; // Проверка флага разрешения стратегии

        //order.close(price); // Закрытие ордера
        //openOrders.remove(order); // Удаление ордера из списка открытых ордеров
        //closedOrders.add(order); // Добавление ордера в список закрытых ордеров
    }
}

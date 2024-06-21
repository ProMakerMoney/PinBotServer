package com.zmn.pinbotserver.model.order;


public class Order {
    String tradingPair; // Торговая пара
    String direction; // Направление (купить, продать)
    long executionTime; // Время исполнения ордера
    double volume; // Объем ордера (в монете)
    double executionPrice; // Цена исполнения (в долларах)
    STATUS status;

    public Order(String tradingPair, String direction, long executionTime, double volume, double executionPrice, STATUS status) {
        this.tradingPair = tradingPair;
        this.direction = direction;
        this.executionTime = executionTime;
        this.volume = volume;
        this.executionPrice = executionPrice;
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Order{Пара = '%s', Направление = '%s', Время = %s, Объем = %.4f, Цена исполнения = %.2f, Тип = %s}",
                tradingPair, direction, executionTime, volume, executionPrice, status);
    }
}

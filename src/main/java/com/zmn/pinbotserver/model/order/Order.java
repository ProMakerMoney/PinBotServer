package com.zmn.pinbotserver.model.order;


import lombok.Getter;

public class Order {
    @Getter
    String tradingPair; // Торговая пара
    @Getter
    String direction; // Направление (купить, продать)
    @Getter
    long executionTime; // Время исполнения ордера
    @Getter
    double volume; // Объем ордера (в монете)
    @Getter
    double executionPrice; // Цена исполнения (в долларах)
    @Getter
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

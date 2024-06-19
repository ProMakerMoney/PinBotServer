package com.zmn.pinbotserver.model.candle;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Entity
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long time;
    @Getter
    @Setter
    private double open;
    @Getter
    @Setter
    private double high;
    @Getter
    @Setter
    private double low;
    @Getter
    @Setter
    private double close;
    @Getter
    @Setter
    private double volume;

    public Candle(long time, double open, double high, double low, double close, double volume) {
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public Candle() {

    }

    public LocalDateTime getTime() {
        return LocalDateTime.ofEpochSecond(time / 1000, 0, ZoneOffset.UTC);
    }

    public void setTime(LocalDateTime dateTime) {
        this.time = dateTime.toEpochSecond(ZoneOffset.UTC) * 1000;
    }

    public String toCsvString() {
        return time + "," + open + "," + high + "," + low + "," + close + "," + volume;
    }


    public double getTypicalPrice() {
        return (high + low + close) / 3;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Candle candle = (Candle) o;
        return time == candle.time;
    }

    @Override
    public int hashCode() {
        return Objects.hash(time);
    }

    @Override
    public String toString() {
        return String.format(" Time: %s\n Open: %.4f\n High: %.4f\n Low: %.4f\n Close: %.4f\n Volume: %.4f",
                getTime(), open, high, low, close, volume);
    }
}

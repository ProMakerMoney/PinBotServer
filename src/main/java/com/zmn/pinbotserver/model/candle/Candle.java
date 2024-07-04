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

    @Getter
    @Setter
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

    @Getter
    @Setter
    private double quoteVolume;

    public Candle(long time, double open, double high, double low, double close, double volume, double quoteVolume) {
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.quoteVolume = quoteVolume;
    }

    public Candle() {
    }

    public LocalDateTime getTimeAsLocalDateTime() {
        return LocalDateTime.ofEpochSecond(time / 1000, 0, ZoneOffset.UTC);
    }

    public void setTimeFromLocalDateTime(LocalDateTime dateTime) {
        this.time = dateTime.toEpochSecond(ZoneOffset.UTC) * 1000;
    }

    public String toCsvString() {
        return time + "," + open + "," + high + "," + low + "," + close + "," + volume + "," + quoteVolume;
    }

    public double getTypicalPrice() {
        return (high + low + close) / 3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Candle candle = (Candle) o;
        return time == candle.time;
    }

    @Override
    public int hashCode() {
        return Objects.hash(time);
    }

    @Override
    public String toString() {
        return String.format("Time: %s\nOpen: %.4f\nHigh: %.4f\nLow: %.4f\nClose: %.4f\nVolume: %.4f\nQuote Volume: %.4f",
                getTimeAsLocalDateTime(), open, high, low, close, volume, quoteVolume);
    }
}
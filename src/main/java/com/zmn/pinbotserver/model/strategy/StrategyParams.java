package com.zmn.pinbotserver.model.strategy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "strategy_params")
@Data
public class StrategyParams {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coin", nullable = false)
    private String coin;

    @Column(name = "timeframe", nullable = false)
    private String timeframe;

    @Column(name = "cci")
    private int cci;

    @Column(name = "ema")
    private int ema;

    @Column(name = "lev")
    private int lev;

    @Column(name = "ratio")
    private double ratio;

    @Column(name = "lot")
    private int lot;

    @Column(name = "date_time")
    private LocalDateTime dateTime;

    @Column(name = "is_work", nullable = false)
    private boolean isWork;
}

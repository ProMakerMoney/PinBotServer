package com.zmn.pinbotserver.model.strategy;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "strategy_stats")
@Data
public class StrategyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "strategy_id", referencedColumnName = "id")
    private StrategyParams strategyParams;

    @Column(name = "trade_count")
    private int tradeCount;

    @Column(name = "profitability")
    private double profitability;

    @Column(name = "return_rate")
    private double returnRate;
}
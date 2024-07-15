package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.strategy.StrategyParamsATR;
import com.zmn.pinbotserver.service.tradeBot.TradeBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trade-bot")
public class TradeBotController {

    @Autowired
    private TradeBotService tradeBotService;

    @PostMapping("/start")
    public ResponseEntity<String> startTradeBot(
            @RequestBody StrategyParamsATR params,
            @RequestParam double initialDeposit,
            @RequestParam double risk) {
        tradeBotService.startTradeBot(params, initialDeposit, risk);
        return ResponseEntity.ok("Trade bot started successfully");
    }

    @PostMapping("/set-mode")
    public ResponseEntity<String> setTradeMode(
            @RequestParam String tradingPair,
            @RequestParam String timeframe,
            @RequestParam String mode) {
        tradeBotService.setTradeMode(tradingPair, timeframe, mode);
        return ResponseEntity.ok("Trade mode set successfully");
    }
}
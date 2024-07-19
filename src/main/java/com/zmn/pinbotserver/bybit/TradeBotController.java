package com.zmn.pinbotserver.bybit;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tradebot")
public class TradeBotController {

    private final TradeBotService tradeBotService;

    @Autowired
    public TradeBotController(TradeBotService tradeBotService) {
        this.tradeBotService = tradeBotService;
    }

    @PostMapping("/addBot")
    public ResponseEntity<TradeBot> addBot(@RequestParam double initialDeposit, @RequestParam double risk) {
        TradeBot bot = tradeBotService.addBot(initialDeposit, risk);
        return ResponseEntity.ok(bot);
    }

    @PostMapping("/addStrategy/{botIndex}")
    public ResponseEntity<StrategyParamsBybit> addStrategyToBot(
            @PathVariable int botIndex,
            @RequestParam String coinName,
            @RequestParam String timeFrame,
            @RequestParam int CCI,
            @RequestParam int EMA,
            @RequestParam int leverage,
            @RequestParam double ratio,
            @RequestParam int maxOrders,
            @RequestParam int ATR,
            @RequestParam double coeff) {
        StrategyParamsBybit strategy = tradeBotService.addStrategyToBot(botIndex, coinName, timeFrame, CCI, EMA, leverage, ratio, maxOrders, ATR, coeff);
        return ResponseEntity.ok(strategy);
    }

    @DeleteMapping("/deleteStrategy/{botIndex}")
    public ResponseEntity<String> deleteStrategy(@PathVariable int botIndex, @RequestParam String coinName, @RequestParam String timeFrame) {
        try {
            tradeBotService.deleteStrategy(botIndex, coinName, timeFrame);
            return ResponseEntity.ok("Стратегия удалена успешно.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/deleteBot/{botIndex}")
    public ResponseEntity<String> deleteBot(@PathVariable int botIndex) {
        try {
            tradeBotService.deleteBot(botIndex);
            return ResponseEntity.ok("Бот удален успешно.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/getBots")
    public ResponseEntity<List<TradeBot>> getTradeBots() {
        List<TradeBot> bots = tradeBotService.getTradeBots();
        return ResponseEntity.ok(bots);
    }

    @PostMapping("/initializeBotsFromFile")
    public ResponseEntity<String> initializeBotsFromFile() {
        return tradeBotService.initializeBotsFromFile();
    }
}

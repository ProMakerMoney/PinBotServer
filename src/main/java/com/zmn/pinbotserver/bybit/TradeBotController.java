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

    @GetMapping("/getBots")
    public ResponseEntity<List<TradeBot>> getTradeBots() {
        List<TradeBot> bots = tradeBotService.getTradeBots();
        return ResponseEntity.ok(bots);
    }
}

package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.bybit.BybitApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final BybitApi bybitApi;

    @Autowired
    public OrderController(BybitApi bybitApi) {
        this.bybitApi = bybitApi;
    }

    /**
     * Endpoint для размещения ордера BUY.
     *
     * @param orderDetails Детали ордера.
     * @return ResponseEntity с результатом.
     */
    @PostMapping("/buy")
    public ResponseEntity<String> placeBuyOrder(@RequestBody Map<String, String> orderDetails) {
        try {
            String symbol = orderDetails.get("symbol");
            String qty = orderDetails.get("qty");
            String price = orderDetails.get("price");
            bybitApi.placeOrder(symbol, "Buy", "Market", qty, null);
            return ResponseEntity.ok("Ордер BUY успешно размещен.");
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при размещении ордера BUY: " + e.getMessage());
        }
    }

    /**
     * Endpoint для размещения ордера SELL.
     *
     * @param orderDetails Детали ордера.
     * @return ResponseEntity с результатом.
     */
    @PostMapping("/sell")
    public ResponseEntity<String> placeSellOrder(@RequestBody Map<String, String> orderDetails) {
        try {
            String symbol = orderDetails.get("symbol");
            String qty = orderDetails.get("qty");
            String price = orderDetails.get("price");
            bybitApi.placeOrder(symbol, "Sell", "Market", qty, null);
            return ResponseEntity.ok("Ордер SELL успешно размещен.");
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при размещении ордера SELL: " + e.getMessage());
        }
    }
}

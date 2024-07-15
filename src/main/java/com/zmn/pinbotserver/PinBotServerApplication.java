package com.zmn.pinbotserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PinBotServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PinBotServerApplication.class, args);
    }
}

package com.zmn.pinbotserver.config;

import com.zmn.pinbotserver.bybit.BybitApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BybitApiConfig {

    @Bean
    public BybitApi bybitApi() {
        String apiKey = "bvQRWwQU8QapNl3Ppl";
        String apiSecret = "P5h8tnabkftRGzrdFV4DXbggI7XJnaaXx6KY";
        boolean isTestnet = false; // или false для основного сетевого режима
        return new BybitApi(apiKey, apiSecret, isTestnet);
    }
}

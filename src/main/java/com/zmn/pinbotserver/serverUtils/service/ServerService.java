package com.zmn.pinbotserver.serverUtils.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ServerService {

    @Autowired
    private final RestTemplate restTemplate;

    public ServerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

//    @Scheduled(fixedRate = 60000)
//    public void serverStatus(){
//        System.out.println("::::: Сервер работает! :::::");
//        LocalDateTime time = LocalDateTime.now(ZoneId.of("UTC"));
//        System.out.println(":::  " + time + " :::");
//    }
}

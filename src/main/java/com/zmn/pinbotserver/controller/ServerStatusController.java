package com.zmn.pinbotserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServerStatusController {

    // Метод для получения статуса сервера
    @GetMapping("/api/server/status")
    public int getServerStatus() {
        // Логика определения статуса сервера
        // Здесь можно добавить любую проверку состояния сервера
        boolean isServerRunning = checkServerStatus();
        // Возвращаем 1, если сервер работает корректно, и 0 в противном случае
        return isServerRunning ? 1 : 0;
    }

    // Метод для проверки состояния сервера
    private boolean checkServerStatus() {
        // Здесь можно добавить любую логику проверки состояния сервера
        // Например, проверка соединения с базой данных, доступность необходимых сервисов и т.д.
        // Для простоты, возвращаем true (сервер работает)
        return true;
    }
}

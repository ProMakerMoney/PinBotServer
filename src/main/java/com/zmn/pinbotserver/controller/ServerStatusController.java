package com.zmn.pinbotserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // Аннотация, обозначающая, что данный класс является контроллером Spring RESTful веб-сервиса
public class ServerStatusController {

    /**
     * Метод для получения статуса сервера
     * @return int статус сервера: 1, если сервер работает корректно, и 0 в противном случае
     */
    @GetMapping("/api/server/status") // Аннотация, обозначающая, что данный метод обрабатывает HTTP GET запросы по указанному URL
    public int getServerStatus() {
        // Логика определения статуса сервера
        // Здесь можно добавить любую проверку состояния сервера
        boolean isServerRunning = checkServerStatus(); // Вызов метода для проверки состояния сервера

        // Возвращаем 1, если сервер работает корректно, и 0 в противном случае
        return isServerRunning ? 1 : 0; // Тернарный оператор: возвращает 1, если isServerRunning истинно, иначе 0
    }

    /**
     * Метод для проверки состояния сервера
     * @return boolean true, если сервер работает корректно, false в противном случае
     */
    private boolean checkServerStatus() {
        // Здесь можно добавить любую логику проверки состояния сервера
        // Например, проверка соединения с базой данных, доступность необходимых сервисов и т.д.

        // Для простоты, возвращаем true (сервер работает)
        return true; // Возвращаем true для обозначения того, что сервер работает корректно
    }
}

package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.user.User;
import com.zmn.pinbotserver.service.UserService;
import com.zmn.pinbotserver.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController // Аннотация, обозначающая, что данный класс является контроллером Spring RESTful веб-сервиса
@RequestMapping("/api/auth") // Базовый URL для всех методов данного контроллера
public class AuthController {

    private final UserService userService; // Сервис для работы с пользователями
    private final JwtUtil jwtUtil; // Утилита для работы с JWT токенами

    // Конструктор для инъекции зависимостей
    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Метод для регистрации нового пользователя
     * @param request карта с параметрами запроса (email, username, password)
     * @return ResponseEntity с сообщением о результате регистрации
     */
    @PostMapping("/register") // Обрабатывает HTTP POST запросы по URL /api/auth/register
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String email = request.get("email"); // Получение email из тела запроса
        String username = request.get("username"); // Получение username из тела запроса
        String password = request.get("password"); // Получение password из тела запроса

        // Регистрация нового пользователя
        Optional<User> user = userService.register(email, username, password);

        // Проверка результата регистрации
        if (user.isPresent()) {
            return ResponseEntity.ok("User registered successfully"); // Возвращаем успешный ответ
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User already exists"); // Возвращаем ошибку, если пользователь уже существует
        }
    }

    /**
     * Метод для аутентификации пользователя
     * @param request карта с параметрами запроса (email, password)
     * @return ResponseEntity с JWT токеном при успешной аутентификации или сообщением об ошибке
     */
    @PostMapping("/login") // Обрабатывает HTTP POST запросы по URL /api/auth/login
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email"); // Получение email из тела запроса
        String password = request.get("password"); // Получение password из тела запроса

        // Аутентификация пользователя
        Optional<User> user = userService.authenticate(email, password);

        // Проверка результата аутентификации
        if (user.isPresent()) {
            String token = jwtUtil.generateToken(email); // Генерация JWT токена
            Map<String, String> response = new HashMap<>();
            response.put("token", token); // Добавление токена в ответ
            return ResponseEntity.ok(response); // Возвращаем успешный ответ с токеном
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials"); // Возвращаем ошибку при неверных учетных данных
        }
    }
}
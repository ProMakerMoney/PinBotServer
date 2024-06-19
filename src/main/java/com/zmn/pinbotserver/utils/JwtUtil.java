package com.zmn.pinbotserver.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;


import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component // Аннотация, обозначающая, что данный класс является компонентом Spring
public class JwtUtil {

    // Секретный ключ для подписи JWT
    private static final String SECRET_KEY = "0JbQsNC90YHQnNCw0YDQutGB0JHRg9C60YDRj9C60YE="; // Замените на ваш секретный ключ

    // Время жизни токена (1 день в миллисекундах)
    private static final long EXPIRATION_TIME = 86400000;

    /**
     * Метод для получения ключа подписи
     * @return Key объект ключа для подписи
     */
    private Key getSigningKey() {
        // Декодирование секретного ключа из Base64
        byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
        // Создание и возврат ключа для HMAC-SHA алгоритма
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Метод для генерации JWT токена
     * @param email email пользователя, для которого генерируется токен
     * @return String JWT токен
     */
    public String generateToken(String email) {
        // Построение и подпись JWT токена
        return Jwts.builder()
                .setSubject(email) // Установка субъекта (email пользователя)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Установка времени жизни токена
                .signWith(getSigningKey()) // Подпись токена ключом
                .compact(); // Компактный формат токена
    }

    /**
     * Метод для извлечения email из JWT токена
     * @param token JWT токен
     * @return String email пользователя
     */
    public String extractEmail(String token) {
        // Парсинг токена и извлечение субъекта (email пользователя)
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // Установка ключа для проверки подписи
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject(); // Получение субъекта из тела токена
    }

    /**
     * Метод для валидации JWT токена
     * @param token JWT токен
     * @param userDetails детали пользователя для проверки
     * @return boolean true, если токен валидный, иначе false
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token); // Извлечение email из токена
        // Проверка соответствия email и срока действия токена
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Метод для проверки истечения срока действия токена
     * @param token JWT токен
     * @return boolean true, если токен истек, иначе false
     */
    private boolean isTokenExpired(String token) {
        // Парсинг токена и проверка времени его истечения
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // Установка ключа для проверки подписи
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .before(new Date()); // Проверка, истек ли токен
    }
}
package com.zmn.pinbotserver.serverUtils.utils;

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

    // Время жизни токена (15 минут в миллисекундах)
    private static final long EXPIRATION_TIME = 1000 * 60 * 6000; // 15 минут

    // Время жизни токена обновления (7 дней в миллисекундах)
    private static final long REFRESH_EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 7; // 7 дней

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
        return createToken(email, EXPIRATION_TIME);
    }

    /**
     * Метод для генерации токена обновления
     * @param email email пользователя, для которого генерируется токен обновления
     * @return String токен обновления
     */
    public String generateRefreshToken(String email) {
        // Построение и подпись токена обновления
        return createToken(email, REFRESH_EXPIRATION_TIME);
    }

    /**
     * Метод для создания JWT токена с заданным временем жизни
     * @param subject субъект (email пользователя)
     * @param expirationTime время жизни токена
     * @return String JWT токен
     */
    private String createToken(String subject, long expirationTime) {
        // Построение и подпись JWT токена
        return Jwts.builder()
                .setSubject(subject) // Установка субъекта (email пользователя)
                .setIssuedAt(new Date()) // Установка времени создания токена
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // Установка времени жизни токена
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
        return getClaims(token).getSubject();
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
        return getClaims(token)
                .getExpiration()
                .before(new Date()); // Проверка, истек ли токен
    }

    /**
     * Метод для извлечения claims из JWT токена
     * @param token JWT токен
     * @return Claims claims из токена
     */
    private Claims getClaims(String token) {
        // Парсинг токена и извлечение claims
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // Установка ключа для проверки подписи
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Метод для валидации токена обновления
     * @param token токен обновления
     * @return boolean true, если токен валидный, иначе false
     */
    public boolean validateRefreshToken(String token) {
        return !isTokenExpired(token);
    }
}
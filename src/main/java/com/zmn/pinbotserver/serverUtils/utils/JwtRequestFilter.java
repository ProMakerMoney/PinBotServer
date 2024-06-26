package com.zmn.pinbotserver.serverUtils.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // Аннотация, обозначающая, что данный класс является компонентом Spring
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired // Автоматическая инъекция зависимостей
    private JwtUtil jwtUtil;

    @Autowired // Автоматическая инъекция зависимостей
    private UserDetailsService userDetailsService;

    /**
     * Метод для фильтрации запросов и проверки JWT токенов
     * @param request HTTP запрос
     * @param response HTTP ответ
     * @param chain цепочка фильтров
     * @throws ServletException при ошибке сервлета
     * @throws IOException при ошибке ввода-вывода
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Получение заголовка Authorization из запроса
        final String authorizationHeader = request.getHeader("Authorization");

        String email = null; // Переменная для хранения email пользователя
        String jwt = null; // Переменная для хранения JWT токена

        // Проверка наличия и формата заголовка Authorization
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Извлечение токена из заголовка (без "Bearer ")
            System.out.println("JWT Token: " + jwt); // Вывод токена для отладки
            try {
                email = jwtUtil.extractEmail(jwt); // Извлечение email из токена
            } catch (Exception e) {
                System.out.println("Error extracting email: " + e.getMessage()); // Вывод ошибки извлечения email
            }
        }

        // Проверка наличия email и отсутствия аутентификации в контексте безопасности
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Загрузка деталей пользователя по email
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);

            // Проверка валидности токена
            if (jwtUtil.validateToken(jwt, userDetails)) {
                // Создание токена аутентификации
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Установка аутентификации в контекст безопасности
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        // Продолжение цепочки фильтров
        chain.doFilter(request, response);
    }
}
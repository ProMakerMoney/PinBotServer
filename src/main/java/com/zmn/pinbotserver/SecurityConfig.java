package com.zmn.pinbotserver;


import com.zmn.pinbotserver.serverUtils.utils.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // Аннотация, обозначающая, что данный класс является конфигурационным для Spring
@EnableWebSecurity // Аннотация, включающая поддержку безопасности в веб-приложении
@EnableMethodSecurity // Аннотация, включающая поддержку безопасности на уровне методов
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter; // Фильтр для обработки JWT токенов
    private final UserDetailsService userDetailsService; // Сервис для работы с деталями пользователя

    // Конструктор для инъекции зависимостей
    public SecurityConfig(JwtRequestFilter jwtRequestFilter, UserDetailsService userDetailsService) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Конфигурация цепочки фильтров безопасности
     * @param http объект для конфигурации безопасности HTTP
     * @return SecurityFilterChain цепочка фильтров безопасности
     * @throws Exception возможные исключения
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Отключение CSRF-защиты
                .csrf(csrf -> csrf.disable())
                // Конфигурация авторизации запросов
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/api/auth/**").permitAll() // Разрешение доступа к эндпоинтам аутентификации
                                .anyRequest().authenticated() // Требование аутентификации для всех остальных запросов
                )
                // Конфигурация управления сессиями
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Использование сессий без состояния
                );

        // Добавление фильтра для обработки JWT токенов перед фильтром аутентификации
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build(); // Построение и возврат цепочки фильтров безопасности
    }

    /**
     * Бин для получения менеджера аутентификации
     * @param authenticationConfiguration конфигурация аутентификации
     * @return AuthenticationManager менеджер аутентификации
     * @throws Exception возможные исключения
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Бин для кодировщика паролей
     * @return PasswordEncoder кодировщик паролей
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Использование BCrypt для кодирования паролей
    }
}
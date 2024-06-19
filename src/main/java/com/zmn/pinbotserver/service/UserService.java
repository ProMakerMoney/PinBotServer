package com.zmn.pinbotserver.service;

import com.zmn.pinbotserver.model.user.User;
import com.zmn.pinbotserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


@Service // Аннотация, обозначающая, что данный класс является сервисом в Spring
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired // Автоматическая инъекция зависимости от UserRepository
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(); // Инициализация кодировщика паролей BCrypt
    }

    /**
     * Метод для регистрации нового пользователя
     * @param email email пользователя
     * @param username имя пользователя
     * @param password пароль пользователя
     * @return Optional с пользователем, если регистрация успешна, иначе пустой Optional
     */
    public Optional<User> register(String email, String username, String password) {
        // Проверка, существует ли уже пользователь с таким email или username
        if (userRepository.findByEmail(email).isPresent() || userRepository.findByUsername(username).isPresent()) {
            return Optional.empty();
        }

        // Создание нового пользователя
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password)); // Хеширование пароля
        userRepository.save(user); // Сохранение пользователя в базе данных
        return Optional.of(user); // Возвращаем Optional с пользователем
    }

    /**
     * Метод для аутентификации пользователя
     * @param email email пользователя
     * @param password пароль пользователя
     * @return Optional с пользователем, если аутентификация успешна, иначе пустой Optional
     */
    public Optional<User> authenticate(String email, String password) {
        // Поиск пользователя по email и проверка пароля
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()));
    }

    /**
     * Метод для загрузки пользователя по его email
     * @param email email пользователя
     * @return UserDetails объект с деталями пользователя
     * @throws UsernameNotFoundException если пользователь не найден
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("USER")
                .build();
    }
}
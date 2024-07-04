package com.zmn.pinbotserver.model.user;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users") // Новое имя таблицы
public class User {

    // Геттеры и сеттеры
    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    private String username;
    @Setter
    @Getter
    private String passwordHash; // Поле для хэшированного пароля
    @Setter
    @Getter
    private String email;
    @Setter
    @Getter
    private LocalDateTime createdAt;
    @Setter
    @Getter
    private LocalDateTime updatedAt;
    private boolean isActive;

    // Конструктор с параметрами
    public User(String username, String passwordHash, String email, LocalDateTime createdAt, LocalDateTime updatedAt, boolean isActive) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isActive = isActive;
    }

    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password) {
        this.passwordHash = password;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

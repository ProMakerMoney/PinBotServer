package com.zmn.pinbotserver.model.user;


import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "USER")
@Accessors(chain = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "У пользователя должна быть указана эл.почта")
    @Email(message = "Некорректная почта")
    @Column(nullable = false, unique = true)
    private String email;

    @NotNull(message = "У пользователя должен быть указан логин")
    @NotBlank(message = "Логин не может быть пустым")
    @Pattern(regexp = "\\S+", message = "В логине не могут находиться пробелы")
    @Column(nullable = false, unique = true)
    private String login;

    @NotNull(message = "Пароль не должен быть пустым")
    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 8, message = "Пароль должен содержать не менее 8 символов")
    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}

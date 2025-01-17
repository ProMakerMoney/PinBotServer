package com.zmn.pinbotserver.model.jwt;

public class JwtRequest {

    private String username;
    private String password;

    // Конструктор по умолчанию
    public JwtRequest() {}

    // Конструктор с параметрами
    public JwtRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Геттеры и сеттеры
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

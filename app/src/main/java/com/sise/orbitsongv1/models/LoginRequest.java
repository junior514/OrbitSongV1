package com.sise.orbitsongv1.models;

public class LoginRequest {
    private String username;
    private String password;
    private String email;
    private String contrasena;

    // Constructor principal
    public LoginRequest(String emailOrUsername, String password) {
        this.username = emailOrUsername;
        this.password = password;
        this.email = emailOrUsername;
        this.contrasena = password;
    }

    // Constructor vacío para Gson
    public LoginRequest() {}

    // Getters y setters
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
package com.sise.orbitsongv1.models;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;

public class User {
    private Long id;

    @SerializedName("email")
    private String email;

    @SerializedName("contrasena")
    private String contrasena;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("apellido")
    private String apellido;

    @SerializedName("telefono")
    private String telefono;

    @SerializedName("fechaNacimiento")
    private String fechaNacimiento; // Usando String para simplicidad

    @SerializedName("genero")
    private String genero;

    @SerializedName("spotifyUserId")
    private String spotifyUserId;

    @SerializedName("spotifyAccessToken")
    private String spotifyAccessToken;

    @SerializedName("spotifyRefreshToken")
    private String spotifyRefreshToken;

    @SerializedName("spotifyTokenExpiresAt")
    private String spotifyTokenExpiresAt;

    @SerializedName("activo")
    private Boolean activo;

    @SerializedName("roles")
    private List<String> roles;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    // Para compatibilidad con código existente
    private String username;
    private String password;

    // Constructores
    public User() {
    }

    // Constructor para login (compatibilidad)
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.email = username; // Asumir que username es email
        this.contrasena = password;
    }

    // Constructor completo
    public User(String email, String contrasena, String nombre) {
        this.email = email;
        this.contrasena = contrasena;
        this.nombre = nombre;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getSpotifyUserId() {
        return spotifyUserId;
    }

    public void setSpotifyUserId(String spotifyUserId) {
        this.spotifyUserId = spotifyUserId;
    }

    public String getSpotifyAccessToken() {
        return spotifyAccessToken;
    }

    public void setSpotifyAccessToken(String spotifyAccessToken) {
        this.spotifyAccessToken = spotifyAccessToken;
    }

    public String getSpotifyRefreshToken() {
        return spotifyRefreshToken;
    }

    public void setSpotifyRefreshToken(String spotifyRefreshToken) {
        this.spotifyRefreshToken = spotifyRefreshToken;
    }

    public String getSpotifyTokenExpiresAt() {
        return spotifyTokenExpiresAt;
    }

    public void setSpotifyTokenExpiresAt(String spotifyTokenExpiresAt) {
        this.spotifyTokenExpiresAt = spotifyTokenExpiresAt;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Getters y setters para compatibilidad
    public String getUsername() {
        return username != null ? username : email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password != null ? password : contrasena;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Métodos de utilidad
    public String getDisplayName() {
        if (nombre != null && !nombre.trim().isEmpty()) {
            return nombre;
        }
        if (email != null && !email.trim().isEmpty()) {
            return email;
        }
        return username;
    }

    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (nombre != null && !nombre.trim().isEmpty()) {
            fullName.append(nombre);
        }
        if (apellido != null && !apellido.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(apellido);
        }
        return fullName.length() > 0 ? fullName.toString() : getDisplayName();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' +
                ", activo=" + activo +
                '}';
    }
}